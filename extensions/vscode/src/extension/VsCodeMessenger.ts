import * as fs from "node:fs";
import * as path from "node:path";

import { ConfigHandler } from "core/config/ConfigHandler";
import { getModelByRole } from "core/config/util";
import { applyCodeBlock } from "core/edit/lazy/applyCodeBlock";
import {
  FromCoreProtocol,
  FromWebviewProtocol,
  ToCoreProtocol,
} from "core/protocol";
import { ToWebviewFromCoreProtocol } from "core/protocol/coreWebview";
import { ToIdeFromWebviewOrCoreProtocol } from "core/protocol/ide";
import { ToIdeFromCoreProtocol } from "core/protocol/ideCore";
import {
  CORE_TO_WEBVIEW_PASS_THROUGH,
  WEBVIEW_TO_CORE_PASS_THROUGH,
} from "core/protocol/passThrough";
import { getBasename } from "core/util";
import { InProcessMessenger, Message } from "core/util/messenger";
import * as vscode from "vscode";

import { stripImages } from "core/util/messageContent";
import { VerticalDiffManager } from "../diff/vertical/manager";
import EditDecorationManager from "../quickEdit/EditDecorationManager";
import {
  getControlPlaneSessionInfo,
  WorkOsAuthProvider,
} from "../stubs/WorkOsAuthProvider";
import { getFullyQualifiedPath } from "../util/util";
import { getExtensionUri } from "../util/vscode";
import { VsCodeIde } from "../VsCodeIde";
import { VsCodeWebviewProtocol } from "../webviewProtocol";

/**
 * A shared messenger class between Core and Webview
 * so we don't have to rewrite some of the handlers
 */
type TODO = any;
type ToIdeOrWebviewFromCoreProtocol = ToIdeFromCoreProtocol &
  ToWebviewFromCoreProtocol;

export class VsCodeMessenger {
  onWebview<T extends keyof FromWebviewProtocol>(
    messageType: T,
    handler: (
      message: Message<FromWebviewProtocol[T][0]>,
    ) => Promise<FromWebviewProtocol[T][1]> | FromWebviewProtocol[T][1],
  ): void {
    void this.webviewProtocol.on(messageType, handler);
  }

  onCore<T extends keyof ToIdeOrWebviewFromCoreProtocol>(
    messageType: T,
    handler: (
      message: Message<ToIdeOrWebviewFromCoreProtocol[T][0]>,
    ) =>
      | Promise<ToIdeOrWebviewFromCoreProtocol[T][1]>
      | ToIdeOrWebviewFromCoreProtocol[T][1],
  ): void {
    this.inProcessMessenger.externalOn(messageType, handler);
  }

  onWebviewOrCore<T extends keyof ToIdeFromWebviewOrCoreProtocol>(
    messageType: T,
    handler: (
      message: Message<ToIdeFromWebviewOrCoreProtocol[T][0]>,
    ) =>
      | Promise<ToIdeFromWebviewOrCoreProtocol[T][1]>
      | ToIdeFromWebviewOrCoreProtocol[T][1],
  ): void {
    this.onWebview(messageType, handler);
    this.onCore(messageType, handler);
  }

  constructor(
    private readonly inProcessMessenger: InProcessMessenger<
      ToCoreProtocol,
      FromCoreProtocol
    >,
    private readonly webviewProtocol: VsCodeWebviewProtocol,
    private readonly ide: VsCodeIde,
    private readonly verticalDiffManagerPromise: Promise<VerticalDiffManager>,
    private readonly configHandlerPromise: Promise<ConfigHandler>,
    private readonly workOsAuthProvider: WorkOsAuthProvider,
    private readonly editDecorationManager: EditDecorationManager,
  ) {
    /** WEBVIEW ONLY LISTENERS **/
    this.onWebview("showFile", (msg) => {
      const fullPath = getFullyQualifiedPath(this.ide, msg.data.filepath);

      if (fullPath) {
        this.ide.openFile(fullPath);
      }
    });

    this.onWebview("vscode/openMoveRightMarkdown", (msg) => {
      vscode.commands.executeCommand(
        "markdown.showPreview",
        vscode.Uri.file(
          path.join(
            getExtensionUri().fsPath,
            "media",
            "move-chat-panel-right.md",
          ),
        ),
      );
    });

    this.onWebview("readRangeInFile", async (msg) => {
      return await vscode.workspace
        .openTextDocument(msg.data.filepath)
        .then((document) => {
          const start = new vscode.Position(0, 0);
          const end = new vscode.Position(5, 0);
          const range = new vscode.Range(start, end);

          const contents = document.getText(range);
          return contents;
        });
    });
    this.onWebview("toggleDevTools", (msg) => {
      vscode.commands.executeCommand("workbench.action.toggleDevTools");
      vscode.commands.executeCommand("devscode.viewLogs");
    });
    this.onWebview("reloadWindow", (msg) => {
      vscode.commands.executeCommand("workbench.action.reloadWindow");
    });
    this.onWebview("focusEditor", (msg) => {
      vscode.commands.executeCommand("workbench.action.focusActiveEditorGroup");
    });
    this.onWebview("toggleFullScreen", (msg) => {
      vscode.commands.executeCommand("devscode.toggleFullScreen");
    });
    // History
    this.onWebview("saveFile", async (msg) => {
      return await ide.saveFile(msg.data.filepath);
    });
    this.onWebview("readFile", async (msg) => {
      return await ide.readFile(msg.data.filepath);
    });
    this.onWebview("showDiff", async (msg) => {
      return await ide.showDiff(
        msg.data.filepath,
        msg.data.newContents,
        msg.data.stepIndex,
      );
    });

    webviewProtocol.on(
      "acceptDiff",
      async ({ data: { filepath, streamId } }) => {
        await vscode.commands.executeCommand(
          "devscode.acceptDiff",
          filepath,
          streamId,
        );
      },
    );

    webviewProtocol.on(
      "rejectDiff",
      async ({ data: { filepath, streamId } }) => {
        await vscode.commands.executeCommand(
          "devscode.rejectDiff",
          filepath,
          streamId,
        );
      },
    );

    this.onWebview("applyToFile", async ({ data }) => {
      webviewProtocol.request("updateApplyState", {
        streamId: data.streamId,
        status: "streaming",
        fileContent: data.text,
      });

      let filepath = data.filepath;

      // If there is a filepath, verify it exists and then open the file
      if (filepath) {
        const fullPath = getFullyQualifiedPath(ide, filepath);

        if (!fullPath) {
          return;
        }

        const fileExists = await this.ide.fileExists(fullPath);

        // If there is no existing file at the path, create it
        if (!fileExists) {
          await this.ide.writeFile(fullPath, "");
          await this.ide.openFile(fullPath);
        }

        await this.ide.openFile(fullPath);
      }

      // Get active text editor
      const editor = vscode.window.activeTextEditor;

      if (!editor) {
        vscode.window.showErrorMessage("No active editor to apply edits to");
        return;
      }

      // If document is empty, insert at 0,0 and finish
      if (!editor.document.getText().trim()) {
        editor.edit((builder) =>
          builder.insert(new vscode.Position(0, 0), data.text),
        );

        void webviewProtocol.request("updateApplyState", {
          streamId: data.streamId,
          status: "closed",
          numDiffs: 0,
          fileContent: data.text,
        });

        return;
      }

      // Get LLM from config
      const configHandler = await configHandlerPromise;
      const config = await configHandler.loadConfig();

      let llm = getModelByRole(config, "applyCodeBlock");

      if (!llm) {
        llm = config.models.find(
          (model) => model.title === data.curSelectedModelTitle,
        );

        if (!llm) {
          vscode.window.showErrorMessage(
            `Model ${data.curSelectedModelTitle} not found in config.`,
          );
          return;
        }
      }

      const fastLlm = getModelByRole(config, "repoMapFileSelection") ?? llm;

      // Generate the diff and pass through diff manager
      const [instant, diffLines] = await applyCodeBlock(
        editor.document.getText(),
        data.text,
        getBasename(editor.document.fileName),
        llm,
        fastLlm,
      );

      const verticalDiffManager = await this.verticalDiffManagerPromise;

      if (instant) {
        await verticalDiffManager.streamDiffLines(
          diffLines,
          instant,
          data.streamId,
        );
      } else {
        const prompt = `The following code was suggested as an edit:\n\`\`\`\n${data.text}\n\`\`\`\nPlease apply it to the previous code.`;
        const fullEditorRange = new vscode.Range(
          0,
          0,
          editor.document.lineCount - 1,
          editor.document.lineAt(editor.document.lineCount - 1).text.length,
        );
        const rangeToApplyTo = editor.selection.isEmpty
          ? fullEditorRange
          : editor.selection;

        await verticalDiffManager.streamEdit(
          prompt,
          llm.title,
          data.streamId,
          undefined,
          undefined,
          rangeToApplyTo,
        );
      }
    });

    this.onWebview("showTutorial", async (msg) => {
      const tutorialPath = path.join(
        getExtensionUri().fsPath,
        "devscode_tutorial.py",
      );
      // Ensure keyboard shortcuts match OS
      if (process.platform !== "darwin") {
        let tutorialContent = fs.readFileSync(tutorialPath, "utf8");
        tutorialContent = tutorialContent
          .replace("⌘", "^")
          .replace("Cmd", "Ctrl");
        fs.writeFileSync(tutorialPath, tutorialContent);
      }

      const doc = await vscode.workspace.openTextDocument(
        vscode.Uri.file(tutorialPath),
      );
      await vscode.window.showTextDocument(doc, {
        preview: false,
      });
    });

    this.onWebview("openUrl", (msg) => {
      vscode.env.openExternal(vscode.Uri.parse(msg.data));
    });

    this.onWebview(
      "overwriteFile",
      async ({ data: { prevFileContent, filepath } }) => {
        if (prevFileContent === null) {
          // TODO: Delete the file
          return;
        }

        await this.ide.openFile(filepath);

        // Get active text editor
        const editor = vscode.window.activeTextEditor;

        if (!editor) {
          vscode.window.showErrorMessage("No active editor to apply edits to");
          return;
        }

        editor.edit((builder) =>
          builder.replace(
            new vscode.Range(
              editor.document.positionAt(0),
              editor.document.positionAt(editor.document.getText().length),
            ),
            prevFileContent,
          ),
        );
      },
    );

    this.onWebview("insertAtCursor", async (msg) => {
      const editor = vscode.window.activeTextEditor;
      if (editor === undefined || !editor.selection) {
        return;
      }

      editor.edit((editBuilder) => {
        editBuilder.replace(
          new vscode.Range(editor.selection.start, editor.selection.end),
          msg.data.text,
        );
      });
    });
    this.onWebview("edit/sendPrompt", async (msg) => {
      const prompt = msg.data.prompt;
      const { start, end } = msg.data.range.range;
      const verticalDiffManager = await verticalDiffManagerPromise;
      const modelTitle = await this.webviewProtocol.request(
        "getDefaultModelTitle",
        undefined,
      );
      const fileAfterEdit = await verticalDiffManager.streamEdit(
        stripImages(prompt),
        modelTitle,
        "edit",
        undefined,
        undefined,
        new vscode.Range(
          new vscode.Position(start.line, start.character),
          new vscode.Position(end.line, end.character),
        ),
      );

      void this.webviewProtocol.request("setEditStatus", {
        status: "accepting",
        fileAfterEdit,
      });
    });
    this.onWebview("edit/acceptReject", async (msg) => {
      const { onlyFirst, accept, filepath } = msg.data;
      if (accept && onlyFirst) {
        // Accept first
        vscode.commands.executeCommand(
          "devscode.acceptVerticalDiffBlock",
          filepath,
          0,
        );
      } else if (accept) {
        vscode.commands.executeCommand("devscode.acceptDiff", filepath);
        // Accept all
      } else if (onlyFirst) {
        // Reject first
        vscode.commands.executeCommand(
          "devscode.rejectVerticalDiffBlock",
          filepath,
          0,
        );
      } else {
        // Reject all
        vscode.commands.executeCommand("devscode.rejectDiff", filepath);
      }
    });
    this.onWebview("edit/escape", async (msg) => {
      this.editDecorationManager.clear();
    });

    /** PASS THROUGH FROM WEBVIEW TO CORE AND BACK **/
    WEBVIEW_TO_CORE_PASS_THROUGH.forEach((messageType) => {
      this.onWebview(messageType, async (msg) => {
        return (await this.inProcessMessenger.externalRequest(
          messageType,
          msg.data,
          msg.messageId,
        )) as TODO;
      });
    });

    /** PASS THROUGH FROM CORE TO WEBVIEW AND BACK **/
    CORE_TO_WEBVIEW_PASS_THROUGH.forEach((messageType) => {
      this.onCore(messageType, async (msg) => {
        return this.webviewProtocol.request(messageType, msg.data);
      });
    });

    /** CORE ONLY LISTENERS **/
    // None right now

    /** BOTH CORE AND WEBVIEW **/
    this.onWebviewOrCore("getIdeSettings", async (msg) => {
      return ide.getIdeSettings();
    });
    this.onWebviewOrCore("getDiff", async (msg) => {
      return ide.getDiff(msg.data.includeUnstaged);
    });
    this.onWebviewOrCore("getTerminalContents", async (msg) => {
      return ide.getTerminalContents();
    });
    this.onWebviewOrCore("getDebugLocals", async (msg) => {
      return ide.getDebugLocals(Number(msg.data.threadIndex));
    });
    this.onWebviewOrCore("getAvailableThreads", async (msg) => {
      return ide.getAvailableThreads();
    });
    this.onWebviewOrCore("getTopLevelCallStackSources", async (msg) => {
      return ide.getTopLevelCallStackSources(
        msg.data.threadIndex,
        msg.data.stackDepth,
      );
    });
    this.onWebviewOrCore("getWorkspaceDirs", async (msg) => {
      return ide.getWorkspaceDirs();
    });
    this.onWebviewOrCore("listFolders", async (msg) => {
      return ide.listFolders();
    });
    this.onWebviewOrCore("writeFile", async (msg) => {
      return ide.writeFile(msg.data.path, msg.data.contents);
    });
    this.onWebviewOrCore("showVirtualFile", async (msg) => {
      return ide.showVirtualFile(msg.data.name, msg.data.content);
    });
    this.onWebviewOrCore("getDevscodeDir", async (msg) => {
      return ide.getDevscodeDir();
    });
    this.onWebviewOrCore("openFile", async (msg) => {
      return ide.openFile(msg.data.path);
    });
    this.onWebviewOrCore("runCommand", async (msg) => {
      await ide.runCommand(msg.data.command);
    });
    this.onWebviewOrCore("getSearchResults", async (msg) => {
      return ide.getSearchResults(msg.data.query);
    });
    this.onWebviewOrCore("subprocess", async (msg) => {
      return ide.subprocess(msg.data.command, msg.data.cwd);
    });
    this.onWebviewOrCore("getProblems", async (msg) => {
      return ide.getProblems(msg.data.filepath);
    });
    this.onWebviewOrCore("getBranch", async (msg) => {
      const { dir } = msg.data;
      return ide.getBranch(dir);
    });
    this.onWebviewOrCore("getOpenFiles", async (msg) => {
      return ide.getOpenFiles();
    });
    this.onWebviewOrCore("getCurrentFile", async () => {
      return ide.getCurrentFile();
    });
    this.onWebviewOrCore("getPinnedFiles", async (msg) => {
      return ide.getPinnedFiles();
    });
    this.onWebviewOrCore("showLines", async (msg) => {
      const { filepath, startLine, endLine } = msg.data;
      return ide.showLines(filepath, startLine, endLine);
    });
    this.onWebviewOrCore("showToast", (msg) => {
      this.ide.showToast(...msg.data);
    });
    this.onWebviewOrCore("getGitHubAuthToken", (msg) =>
      ide.getGitHubAuthToken(msg.data),
    );
    this.onWebviewOrCore("getControlPlaneSessionInfo", async (msg) => {
      return getControlPlaneSessionInfo(msg.data.silent);
    });
    this.onWebviewOrCore("logoutOfControlPlane", async (msg) => {
      const sessions = await this.workOsAuthProvider.getSessions();
      await Promise.all(
        sessions.map((session) => workOsAuthProvider.removeSession(session.id)),
      );
      vscode.commands.executeCommand(
        "setContext",
        "devscode.isSignedInToControlPlane",
        false,
      );
    });
  }
}