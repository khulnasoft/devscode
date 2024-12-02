package com.github.khulnasoft.devscodeintellijextension.activities

import com.github.khulnasoft.devscodeintellijextension.auth.AuthListener
import com.github.khulnasoft.devscodeintellijextension.auth.DevscodeAuthService
import com.github.khulnasoft.devscodeintellijextension.auth.ControlPlaneSessionInfo
import com.github.khulnasoft.devscodeintellijextension.constants.getDevscodeGlobalPath
import com.github.khulnasoft.devscodeintellijextension.`devscode`.*
import com.github.khulnasoft.devscodeintellijextension.listeners.DevscodePluginSelectionListener
import com.github.khulnasoft.devscodeintellijextension.services.DevscodeExtensionSettings
import com.github.khulnasoft.devscodeintellijextension.services.DevscodePluginService
import com.github.khulnasoft.devscodeintellijextension.services.SettingsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.*
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import javax.swing.*
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent

fun showTutorial(project: Project) {
    val tutorialFileName = getTutorialFileName()

    DevscodePluginStartupActivity::class.java.getClassLoader().getResourceAsStream(tutorialFileName)
        .use { `is` ->
            if (`is` == null) {
                throw IOException("Resource not found: $tutorialFileName")
            }
            var content = StreamUtil.readText(`is`, StandardCharsets.UTF_8)
            if (!System.getProperty("os.name").lowercase().contains("mac")) {
                content = content.replace("⌘", "⌃")
            }
            val filepath = Paths.get(getDevscodeGlobalPath(), tutorialFileName).toString()
            File(filepath).writeText(content)
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filepath)

            ApplicationManager.getApplication().invokeLater {
                if (virtualFile != null) {
                    FileEditorManager.getInstance(project).openFile(virtualFile, true)
                }
            }
        }
}

private fun getTutorialFileName(): String {
    val appName = ApplicationNamesInfo.getInstance().fullProductName.lowercase()
    return when {
        appName.contains("intellij") -> "devscode_tutorial.java"
        appName.contains("pycharm") -> "devscode_tutorial.py"
        appName.contains("webstorm") -> "devscode_tutorial.ts"
        else -> "devscode_tutorial.py" // Default to Python tutorial
    }
}

class DevscodePluginStartupActivity : StartupActivity, DumbAware {

    override fun runActivity(project: Project) {
        removeShortcutFromAction(getPlatformSpecificKeyStroke("J"))
        removeShortcutFromAction(getPlatformSpecificKeyStroke("shift J"))
        removeShortcutFromAction(getPlatformSpecificKeyStroke("I"))
        initializePlugin(project)
    }

    private fun getPlatformSpecificKeyStroke(key: String): String {
        val osName = System.getProperty("os.name").toLowerCase()
        val modifier = if (osName.contains("mac")) "meta" else "control"
        return "$modifier $key"
    }

    private fun removeShortcutFromAction(shortcut: String) {
        val keymap = KeymapManager.getInstance().activeKeymap
        val keyStroke = KeyStroke.getKeyStroke(shortcut)
        val actionIds = keymap.getActionIds(keyStroke)

        // If Devscode has been re-assigned to another key, don't remove the shortcut
        if (!actionIds.any { it.startsWith("devscode") }) {
            return
        }

        for (actionId in actionIds) {
            if (actionId.startsWith("devscode")) {
                devscode
            }
            val shortcuts = keymap.getShortcuts(actionId)
            for (shortcut in shortcuts) {
                if (shortcut is KeyboardShortcut && shortcut.firstKeyStroke == keyStroke) {
                    keymap.removeShortcut(actionId, shortcut)
                }
            }
        }
    }

    private fun initializePlugin(project: Project) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val devscodePluginService = ServiceManager.getService(
            project,
            DevscodePluginService::class.java
        )

        coroutineScope.launch {
            val settings =
                ServiceManager.getService(DevscodeExtensionSettings::class.java)
            if (!settings.devscodeState.shownWelcomeDialog) {
                settings.devscodeState.shownWelcomeDialog = true
                // Open tutorial file
                showTutorial(project)
            }

            settings.addRemoteSyncJob()

            val ideProtocolClient = IdeProtocolClient(
                devscodePluginService,
                coroutineScope,
                project.basePath,
                project
            )

            devscodePluginService.ideProtocolClient = ideProtocolClient

            // Listen to changes to settings so the core can reload remote configuration
            val connection = ApplicationManager.getApplication().messageBus.connect()
            connection.subscribe(SettingsListener.TOPIC, object : SettingsListener {
                override fun settingsUpdated(settings: DevscodeExtensionSettings.DevscodeState) {
                    devscodePluginService.coreMessenger?.request("config/ideSettingsUpdate", settings, null) { _ -> }
                    devscodePluginService.sendToWebview(
                        "didChangeIdeSettings", mapOf(
                            "settings" to mapOf(
                                "remoteConfigServerUrl" to settings.remoteConfigServerUrl,
                                "remoteConfigSyncPeriod" to settings.remoteConfigSyncPeriod,
                                "userToken" to settings.userToken,
                                "enableControlServerBeta" to settings.enableDevscodeTeamsBeta
                            )
                        )
                    )
                }
            })

            // Handle file changes and deletions - reindex
            connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    // Collect all relevant paths for deletions
                    val deletedPaths = events.filterIsInstance<VFileDeleteEvent>()
                        .map { event -> event.file.path.split("/").dropLast(1).joinToString("/") }

                    // Collect all relevant paths for content changes
                    val changedPaths = events.filterIsInstance<VFileContentChangeEvent>()
                        .map { event -> event.file.path.split("/").dropLast(1).joinToString("/") }

                    // Combine both lists of paths for re-indexing
                    val allPaths = deletedPaths + changedPaths

                    // Create a data map if there are any paths to re-index
                    if (allPaths.isNotEmpty()) {
                        val data = mapOf("files" to allPaths)
                        devscodePluginService.coreMessenger?.request("index/forceReIndexFiles", data, null) { _ -> }
                    }
                }
            })

            // Listen for clicking settings button to start the auth flow
            val authService = service<DevscodeAuthService>()
            val initialSessionInfo = authService.loadControlPlaneSessionInfo()

            if (initialSessionInfo != null) {
                val data = mapOf(
                    "sessionInfo" to initialSessionInfo
                )
                devscodePluginService.coreMessenger?.request("didChangeControlPlaneSessionInfo", data, null) { _ -> }
                devscodePluginService.sendToWebview("didChangeControlPlaneSessionInfo", data)
            }

            connection.subscribe(AuthListener.TOPIC, object : AuthListener {
                override fun startAuthFlow() {
                    authService.startAuthFlow(project)
                }

                override fun handleUpdatedSessionInfo(sessionInfo: ControlPlaneSessionInfo?) {
                    val data = mapOf(
                        "sessionInfo" to sessionInfo
                    )
                    devscodePluginService.coreMessenger?.request(
                        "didChangeControlPlaneSessionInfo",
                        data,
                        null
                    ) { _ -> }
                    devscodePluginService.sendToWebview("didChangeControlPlaneSessionInfo", data)
                }
            })

            val listener =
                DevscodePluginSelectionListener(
                    coroutineScope,
                )

            // Reload the WebView
            devscodePluginService?.let { pluginService ->
                val allModulePaths = ModuleManager.getInstance(project).modules
                    .flatMap { module -> ModuleRootManager.getInstance(module).contentRoots.map { it.path } }
                    .map { Paths.get(it).normalize() }

                val topLevelModulePaths = allModulePaths
                    .filter { modulePath -> allModulePaths.none { it != modulePath && modulePath.startsWith(it) } }
                    .map { it.toString() }

                pluginService.workspacePaths = topLevelModulePaths.toTypedArray()
            }

            EditorFactory.getInstance().eventMulticaster.addSelectionListener(
                listener,
                DevscodePluginDisposable.getInstance(project)
            )

            val coreMessengerManager = CoreMessengerManager(project, ideProtocolClient, coroutineScope)
            devscodePluginService.coreMessengerManager = coreMessengerManager
        }
    }
}