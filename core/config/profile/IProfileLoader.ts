// ProfileHandlers manage the loading of a config, allowing us to abstract over different ways of getting to a DevscodeConfig

import { DevscodeConfig } from "../../index.js";
import { ConfigResult } from "../load.js";

// After we have the DevscodeConfig, the ConfigHandler takes care of everything else (loading models, lifecycle, etc.)
export interface IProfileLoader {
  profileTitle: string;
  profileId: string;
  doLoadConfig(): Promise<ConfigResult<DevscodeConfig>>;
  setIsActive(isActive: boolean): void;
}
