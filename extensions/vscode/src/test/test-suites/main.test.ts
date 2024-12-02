import assert from "node:assert";

import { describe, test } from "mocha";
import * as vscode from "vscode";

import { VsCodeExtension } from "../../extension/VsCodeExtension";

describe("Extension Test Suite", () => {
  vscode.window.showInformationMessage("Start all tests.");

  // test("Make sure that nothing breaks after 10 seconds", async () => {
  //   await new Promise((resolve) => setTimeout(resolve, 10_000));
  // });

  test("Get the default model from webview", async () => {
    const devscodeExtensionApi =
      vscode.extensions.getExtension("devscode.devscode");
    const extension: VsCodeExtension = devscodeExtensionApi?.exports.extension;
    await new Promise((resolve) => setTimeout(resolve, 400));
    await vscode.commands.executeCommand("devscode.focusDevscodeInput");
    await new Promise((resolve) => setTimeout(resolve, 400));
    const title = await (
      await extension.webviewProtocolPromise
    ).request("getDefaultModelTitle", undefined);
    assert.strictEqual(typeof title, "string");
    assert.strictEqual(title, "Test Model");
  });
});
