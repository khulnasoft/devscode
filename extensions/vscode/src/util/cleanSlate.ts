import fs from "fs";

import { getDevscodeGlobalPath } from "core/util/paths";
import { ExtensionContext } from "vscode";

/**
 * Clear all Devscode-related artifacts to simulate a brand new user
 */
export function cleanSlate(context: ExtensionContext) {
  // Commented just to be safe
  // // Remove ~/.devscode
  // const devscodePath = getDevscodeGlobalPath();
  // if (fs.existsSync(devscodePath)) {
  //   fs.rmSync(devscodePath, { recursive: true, force: true });
  // }
  // // Clear extension's globalState
  // context.globalState.keys().forEach((key) => {
  //   context.globalState.update(key, undefined);
  // });
}
