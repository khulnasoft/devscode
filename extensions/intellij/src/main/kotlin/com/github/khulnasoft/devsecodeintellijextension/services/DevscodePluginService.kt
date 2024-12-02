package com.github.khulnasoft.devscodeintellijextension.services

import com.github.khulnasoft.devscodeintellijextension.`devscode`.CoreMessenger
import com.github.khulnasoft.devscodeintellijextension.`devscode`.CoreMessengerManager
import com.github.khulnasoft.devscodeintellijextension.`devscode`.IdeProtocolClient
import com.github.khulnasoft.devscodeintellijextension.`devscode`.uuid
import com.github.khulnasoft.devscodeintellijextension.toolWindow.DevscodeBrowser
import com.github.khulnasoft.devscodeintellijextension.toolWindow.DevscodePluginToolWindowFactory
import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.executeJavaScriptAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

@Service(Service.Level.PROJECT)
class DevscodePluginService(project: Project) : Disposable, DumbAware {
    val coroutineScope = CoroutineScope(Dispatchers.Main)
    var devscodePluginWindow: DevscodePluginToolWindowFactory.DevscodePluginWindow? = null

    var ideProtocolClient: IdeProtocolClient? = null

    var coreMessengerManager: CoreMessengerManager? = null
    val coreMessenger: CoreMessenger?
        get() = coreMessengerManager?.coreMessenger

    var workspacePaths: Array<String>? = null
    var windowId: String = UUID.randomUUID().toString()

    override fun dispose() {
        coroutineScope.cancel()
        coreMessenger?.coroutineScope?.let {
            it.cancel()
            coreMessenger?.killSubProcess()
        }
    }

    fun sendToWebview(
        messageType: String,
        data: Any?,
        messageId: String = uuid()
    ) {
        devscodePluginWindow?.browser?.sendToWebview(messageType, data, messageId)
    }
}