import { workspace } from "vscode";

export const DEVSCODE_WORKSPACE_KEY = "devscode";

export function getDevscodeWorkspaceConfig() {
  return workspace.getConfiguration(DEVSCODE_WORKSPACE_KEY);
}
