import {
  DevscodeConfig,
  DevscodeRcJson,
  IDE,
  IdeSettings,
  SerializedDevscodeConfig,
} from "../../";
import { DevscodeProxyReranker } from "../../context/rerankers/DevscodeProxyReranker.js";
import { ControlPlaneProxyInfo } from "../../control-plane/analytics/IAnalyticsProvider.js";
import { ControlPlaneClient } from "../../control-plane/client.js";
import { controlPlaneEnv } from "../../control-plane/env.js";
import { TeamAnalytics } from "../../control-plane/TeamAnalytics.js";
import DevscodeProxyEmbeddingsProvider from "../../indexing/embeddings/DevscodeProxyEmbeddingsProvider";
import DevscodeProxy from "../../llm/llms/stubs/DevscodeProxy";
import { Telemetry } from "../../util/posthog";
import { TTS } from "../../util/tts";
import { ConfigResult, loadFullConfigNode } from "../load";

export default async function doLoadConfig(
  ide: IDE,
  ideSettingsPromise: Promise<IdeSettings>,
  controlPlaneClient: ControlPlaneClient,
  writeLog: (message: string) => Promise<void>,
  overrideConfigJson: SerializedDevscodeConfig | undefined,
  workspaceId?: string,
): Promise<ConfigResult<DevscodeConfig>> {
  const workspaceConfigs = await getWorkspaceConfigs(ide);
  const ideInfo = await ide.getIdeInfo();
  const uniqueId = await ide.getUniqueId();
  const ideSettings = await ideSettingsPromise;
  const workOsAccessToken = await controlPlaneClient.getAccessToken();

  let {
    config: newConfig,
    errors,
    configLoadInterrupted,
  } = await loadFullConfigNode(
    ide,
    workspaceConfigs,
    ideSettings,
    ideInfo.ideType,
    uniqueId,
    writeLog,
    workOsAccessToken,
    overrideConfigJson,
  );

  if (configLoadInterrupted || !newConfig) {
    return { errors, config: newConfig, configLoadInterrupted: true };
  }

  newConfig.allowAnonymousTelemetry =
    newConfig.allowAnonymousTelemetry && (await ide.isTelemetryEnabled());

  // Setup telemetry only after (and if) we know it is enabled
  await Telemetry.setup(
    newConfig.allowAnonymousTelemetry ?? true,
    await ide.getUniqueId(),
    ideInfo,
  );

  // TODO: pass config to pre-load non-system TTS models
  await TTS.setup();

  // Set up control plane proxy if configured
  const controlPlane = (newConfig as any).controlPlane;
  const useOnPremProxy =
    controlPlane?.useDevscodeForTeamsProxy === false && controlPlane?.proxyUrl;
  let controlPlaneProxyUrl: string = useOnPremProxy
    ? controlPlane?.proxyUrl
    : controlPlaneEnv.DEFAULT_CONTROL_PLANE_PROXY_URL;

  if (!controlPlaneProxyUrl.endsWith("/")) {
    controlPlaneProxyUrl += "/";
  }
  const controlPlaneProxyInfo = {
    workspaceId,
    controlPlaneProxyUrl,
    workOsAccessToken,
  };

  if (newConfig.analytics) {
    await TeamAnalytics.setup(
      newConfig.analytics as any, // TODO: Need to get rid of index.d.ts once and for all
      uniqueId,
      ideInfo.extensionVersion,
      controlPlaneClient,
      controlPlaneProxyInfo,
    );
  }

  newConfig = await injectControlPlaneProxyInfo(
    newConfig,
    controlPlaneProxyInfo,
  );

  return { config: newConfig, errors, configLoadInterrupted: false };
}

// Pass ControlPlaneProxyInfo to objects that need it
async function injectControlPlaneProxyInfo(
  config: DevscodeConfig,
  info: ControlPlaneProxyInfo,
): Promise<DevscodeConfig> {
  [...config.models, ...(config.tabAutocompleteModels ?? [])].forEach(
    async (model) => {
      if (model.providerName === "devscode-proxy") {
        (model as DevscodeProxy).controlPlaneProxyInfo = info;
      }
    },
  );

  if (config.embeddingsProvider?.providerName === "devscode-proxy") {
    (
      config.embeddingsProvider as DevscodeProxyEmbeddingsProvider
    ).controlPlaneProxyInfo = info;
  }

  if (config.reranker?.name === "devscode-proxy") {
    (config.reranker as DevscodeProxyReranker).controlPlaneProxyInfo = info;
  }

  return config;
}

async function getWorkspaceConfigs(ide: IDE): Promise<DevscodeRcJson[]> {
  const ideInfo = await ide.getIdeInfo();
  let workspaceConfigs: DevscodeRcJson[] = [];

  try {
    workspaceConfigs = await ide.getWorkspaceConfigs();

    // Config is sent over the wire from JB so we need to parse it
    if (ideInfo.ideType === "jetbrains") {
      workspaceConfigs = (workspaceConfigs as any).map(JSON.parse);
    }
  } catch (e) {
    console.debug("Failed to load workspace configs: ", e);
  }

  return workspaceConfigs;
}
