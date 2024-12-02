import * as fs from "fs";
import * as os from "os";
import { pathToFileURL } from "url";
import * as path from "path";
import * as JSONC from "comment-json";
import dotenv from "dotenv";

import { IdeType, SerializedDevscodeConfig } from "../";
import { defaultConfig, defaultConfigJetBrains } from "../config/default";
import Types from "../config/types";

dotenv.config();

const DEVSCODE_GLOBAL_DIR =
  process.env.DEVSCODE_GLOBAL_DIR ?? path.join(os.homedir(), ".devscode");

export const DEFAULT_CONFIG_TS_CONTENTS = `export function modifyConfig(config: Config): Config {
  return config;
}`;

export function getChromiumPath(): string {
  return path.join(getDevscodeUtilsPath(), ".chromium-browser-snapshots");
}

export function getDevscodeUtilsPath(): string {
  const utilsPath = path.join(getDevscodeGlobalPath(), ".utils");
  if (!fs.existsSync(utilsPath)) {
    fs.mkdirSync(utilsPath);
  }
  return utilsPath;
}

export function getGlobalDevscodeIgnorePath(): string {
  const devscodeIgnorePath = path.join(
    getDevscodeGlobalPath(),
    ".devscodeignore",
  );
  if (!fs.existsSync(devscodeIgnorePath)) {
    fs.writeFileSync(devscodeIgnorePath, "");
  }
  return devscodeIgnorePath;
}

/*
  Deprecated, replace with getDevscodeGlobalUri where possible
*/
export function getDevscodeGlobalPath(): string {
  // This is ~/.devscode on mac/linux
  const devscodePath = DEVSCODE_GLOBAL_DIR;
  if (!fs.existsSync(devscodePath)) {
    fs.mkdirSync(devscodePath);
  }
  return devscodePath;
}

export function getDevscodeGlobalUri(): string {
  return pathToFileURL(DEVSCODE_GLOBAL_DIR).href;
}

export function getSessionsFolderPath(): string {
  const sessionsPath = path.join(getDevscodeGlobalPath(), "sessions");
  if (!fs.existsSync(sessionsPath)) {
    fs.mkdirSync(sessionsPath);
  }
  return sessionsPath;
}

export function getIndexFolderPath(): string {
  const indexPath = path.join(getDevscodeGlobalPath(), "index");
  if (!fs.existsSync(indexPath)) {
    fs.mkdirSync(indexPath);
  }
  return indexPath;
}

export function getGlobalContextFilePath(): string {
  return path.join(getIndexFolderPath(), "globalContext.json");
}

export function getSessionFilePath(sessionId: string): string {
  return path.join(getSessionsFolderPath(), `${sessionId}.json`);
}

export function getSessionsListPath(): string {
  const filepath = path.join(getSessionsFolderPath(), "sessions.json");
  if (!fs.existsSync(filepath)) {
    fs.writeFileSync(filepath, JSON.stringify([]));
  }
  return filepath;
}

export function getConfigJsonPath(ideType: IdeType = "vscode"): string {
  const p = path.join(getDevscodeGlobalPath(), "config.json");
  if (!fs.existsSync(p)) {
    if (ideType === "jetbrains") {
      fs.writeFileSync(p, JSON.stringify(defaultConfigJetBrains, null, 2));
    } else {
      fs.writeFileSync(p, JSON.stringify(defaultConfig, null, 2));
    }
  }
  return p;
}

export function getConfigJsonUri(): string {
  return getDevscodeGlobalUri() + "/config.json";
}

export function getConfigTsPath(): string {
  const p = path.join(getDevscodeGlobalPath(), "config.ts");
  if (!fs.existsSync(p)) {
    fs.writeFileSync(p, DEFAULT_CONFIG_TS_CONTENTS);
  }

  const typesPath = path.join(getDevscodeGlobalPath(), "types");
  if (!fs.existsSync(typesPath)) {
    fs.mkdirSync(typesPath);
  }
  const corePath = path.join(typesPath, "core");
  if (!fs.existsSync(corePath)) {
    fs.mkdirSync(corePath);
  }
  const packageJsonPath = path.join(getDevscodeGlobalPath(), "package.json");
  if (!fs.existsSync(packageJsonPath)) {
    fs.writeFileSync(
      packageJsonPath,
      JSON.stringify({
        name: "devscode-config",
        version: "1.0.0",
        description: "My Devscode Configuration",
        main: "config.js",
      }),
    );
  }

  fs.writeFileSync(path.join(corePath, "index.d.ts"), Types);
  return p;
}

export function getConfigJsPath(): string {
  // Do not create automatically
  return path.join(getDevscodeGlobalPath(), "out", "config.js");
}

export function getTsConfigPath(): string {
  const tsConfigPath = path.join(getDevscodeGlobalPath(), "tsconfig.json");
  if (!fs.existsSync(tsConfigPath)) {
    fs.writeFileSync(
      tsConfigPath,
      JSON.stringify(
        {
          compilerOptions: {
            target: "ESNext",
            useDefineForClassFields: true,
            lib: ["DOM", "DOM.Iterable", "ESNext"],
            allowJs: true,
            skipLibCheck: true,
            esModuleInterop: false,
            allowSyntheticDefaultImports: true,
            strict: true,
            forceConsistentCasingInFileNames: true,
            module: "System",
            moduleResolution: "Node",
            noEmit: false,
            noEmitOnError: false,
            outFile: "./out/config.js",
            typeRoots: ["./node_modules/@types", "./types"],
          },
          include: ["./config.ts"],
        },
        null,
        2,
      ),
    );
  }
  return tsConfigPath;
}

export function getDevscodeRcPath(): string {
  // Disable indexing of the config folder to prevent infinite loops
  const devscodercPath = path.join(getDevscodeGlobalPath(), ".devscoderc.json");
  if (!fs.existsSync(devscodercPath)) {
    fs.writeFileSync(
      devscodercPath,
      JSON.stringify(
        {
          disableIndexing: true,
        },
        null,
        2,
      ),
    );
  }
  return devscodercPath;
}

export function devDataPath(): string {
  const sPath = path.join(getDevscodeGlobalPath(), "dev_data");
  if (!fs.existsSync(sPath)) {
    fs.mkdirSync(sPath);
  }
  return sPath;
}

export function getDevDataSqlitePath(): string {
  return path.join(devDataPath(), "devdata.sqlite");
}

export function getDevDataFilePath(fileName: string): string {
  return path.join(devDataPath(), `${fileName}.jsonl`);
}

export function editConfigJson(
  callback: (config: SerializedDevscodeConfig) => SerializedDevscodeConfig,
): void {
  const config = fs.readFileSync(getConfigJsonPath(), "utf8");
  let configJson = JSONC.parse(config);
  // Check if it's an object
  if (typeof configJson === "object" && configJson !== null) {
    configJson = callback(configJson as any) as any;
    fs.writeFileSync(getConfigJsonPath(), JSONC.stringify(configJson, null, 2));
  } else {
    console.warn("config.json is not a valid object");
  }
}

function getMigrationsFolderPath(): string {
  const migrationsPath = path.join(getDevscodeGlobalPath(), ".migrations");
  if (!fs.existsSync(migrationsPath)) {
    fs.mkdirSync(migrationsPath);
  }
  return migrationsPath;
}

export async function migrate(
  id: string,
  callback: () => void | Promise<void>,
  onAlreadyComplete?: () => void,
) {
  if (process.env.NODE_ENV === "test") {
    return await Promise.resolve(callback());
  }

  const migrationsPath = getMigrationsFolderPath();
  const migrationPath = path.join(migrationsPath, id);

  if (!fs.existsSync(migrationPath)) {
    try {
      console.log(`Running migration: ${id}`);

      fs.writeFileSync(migrationPath, "");
      await Promise.resolve(callback());
    } catch (e) {
      console.warn(`Migration ${id} failed`, e);
    }
  } else if (onAlreadyComplete) {
    onAlreadyComplete();
  }
}

export function getIndexSqlitePath(): string {
  return path.join(getIndexFolderPath(), "index.sqlite");
}

export function getLanceDbPath(): string {
  return path.join(getIndexFolderPath(), "lancedb");
}

export function getTabAutocompleteCacheSqlitePath(): string {
  return path.join(getIndexFolderPath(), "autocompleteCache.sqlite");
}

export function getDocsSqlitePath(): string {
  return path.join(getIndexFolderPath(), "docs.sqlite");
}

export function getRemoteConfigsFolderPath(): string {
  const dir = path.join(getDevscodeGlobalPath(), ".configs");
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir);
  }
  return dir;
}

export function getPathToRemoteConfig(remoteConfigServerUrl: string): string {
  let url: URL | undefined = undefined;
  try {
    url =
      typeof remoteConfigServerUrl !== "string" || remoteConfigServerUrl === ""
        ? undefined
        : new URL(remoteConfigServerUrl);
  } catch (e) {}
  const dir = path.join(getRemoteConfigsFolderPath(), url?.hostname ?? "None");
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir);
  }
  return dir;
}

export function internalBetaPathExists(): boolean {
  const sPath = path.join(getDevscodeGlobalPath(), ".internal_beta");
  return fs.existsSync(sPath);
}

export function getConfigJsonPathForRemote(
  remoteConfigServerUrl: string,
): string {
  return path.join(getPathToRemoteConfig(remoteConfigServerUrl), "config.json");
}

export function getConfigJsPathForRemote(
  remoteConfigServerUrl: string,
): string {
  return path.join(getPathToRemoteConfig(remoteConfigServerUrl), "config.js");
}

export function getDevscodeDotEnv(): { [key: string]: string } {
  const filepath = path.join(getDevscodeGlobalPath(), ".env");
  if (fs.existsSync(filepath)) {
    return dotenv.parse(fs.readFileSync(filepath));
  }
  return {};
}

export function getLogsDirPath(): string {
  const logsPath = path.join(getDevscodeGlobalPath(), "logs");
  if (!fs.existsSync(logsPath)) {
    fs.mkdirSync(logsPath);
  }
  return logsPath;
}

export function getCoreLogsPath(): string {
  return path.join(getLogsDirPath(), "core.log");
}

export function getPromptLogsPath(): string {
  return path.join(getLogsDirPath(), "prompt.log");
}

export function getGlobalPromptsPath(): string {
  return path.join(getDevscodeGlobalPath(), "prompts");
}

export function readAllGlobalPromptFiles(
  folderPath: string = getGlobalPromptsPath(),
): { path: string; content: string }[] {
  if (!fs.existsSync(folderPath)) {
    return [];
  }
  const files = fs.readdirSync(folderPath);
  const promptFiles: { path: string; content: string }[] = [];
  files.forEach((file) => {
    const filepath = path.join(folderPath, file);
    const stats = fs.statSync(filepath);

    if (stats.isDirectory()) {
      const nestedPromptFiles = readAllGlobalPromptFiles(filepath);
      promptFiles.push(...nestedPromptFiles);
    } else {
      const content = fs.readFileSync(filepath, "utf8");
      promptFiles.push({ path: filepath, content });
    }
  });

  return promptFiles;
}

export function getRepoMapFilePath(): string {
  return path.join(getDevscodeUtilsPath(), "repo_map.txt");
}

export function getEsbuildBinaryPath(): string {
  return path.join(getDevscodeUtilsPath(), "esbuild");
}

export function setupInitialDotDevscodeDirectory() {
  const devDataTypes = [
    "chat",
    "autocomplete",
    "quickEdit",
    "tokens_generated",
  ];
  devDataTypes.forEach((p) => {
    const devDataPath = getDevDataFilePath(p);
    if (!fs.existsSync(devDataPath)) {
      fs.writeFileSync(devDataPath, "");
    }
  });
}