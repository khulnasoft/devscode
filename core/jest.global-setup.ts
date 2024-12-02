import fs from "fs";
import path from "path";

export default async function () {
  process.env.DEVSCODE_GLOBAL_DIR = path.join(__dirname, ".devscode-test");
  if (fs.existsSync(process.env.DEVSCODE_GLOBAL_DIR)) {
    fs.rmdirSync(process.env.DEVSCODE_GLOBAL_DIR, { recursive: true });
  }
}
