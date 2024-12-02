package com.github.khulnasoft.devscodeintellijextension.toolWindow

import com.github.khulnasoft.devscodeintellijextension.activities.DevscodePluginDisposable
import com.github.khulnasoft.devscodeintellijextension.activities.showTutorial
import com.github.khulnasoft.devscodeintellijextension.constants.MessageTypes
import com.github.khulnasoft.devscodeintellijextension.constants.getConfigJsonPath
import com.github.khulnasoft.devscodeintellijextension.`devscode`.*
import com.github.khulnasoft.devscodeintellijextension.factories.CustomSchemeHandlerFactory
import com.github.khulnasoft.devscodeintellijextension.services.DevscodeExtensionSettings
import com.github.khulnasoft.devscodeintellijextension.services.DevscodePluginService
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.*
import kotlinx.coroutines.*
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.handler.CefLoadHandlerAdapter

class DevscodeBrowser(val project: Project, url: String) {
    private val coroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )

    private val heightChangeListeners = mutableListOf<(Int) -> Unit>()

    private val PASS_THROUGH_TO_CORE = listOf(
        "update/modelChange",
        "ping",
        "abort",
        "history/list",
        "history/delete",
        "history/load",
        "history/save",
        "devdata/log",
        "config/addOpenAiKey",
        "config/addModel",
        "config/ideSettingsUpdate",
        "config/getSerializedProfileInfo",
        "config/deleteModel",
        "config/newPromptFile",
        "config/reload",
        "context/getContextItems",
        "context/loadSubmenuItems",
        "context/addDocs",
        "autocomplete/complete",
        "autocomplete/cancel",
        "autocomplete/accept",
        "command/run",
        "llm/complete",
        "llm/streamComplete",
        "llm/streamChat",
        "llm/listModels",
        "streamDiffLines",
        "stats/getTokensPerDay",
        "stats/getTokensPerModel",
        "index/setPaused",
        "index/forceReIndex",
        "index/forceReIndexFiles",
        "index/indexingProgressBarInitialized",
        "completeOnboarding",
        "addAutocompleteModel",
        "config/listProfiles",
        "profiles/switch",
        "didChangeSelectedProfile",
        "context/getSymbolsForFiles",
    )

    private fun registerAppSchemeHandler() {
        CefApp.getInstance().registerSchemeHandlerFactory(
            "http",
            "devscode",
            CustomSchemeHandlerFactory()
        )
    }

    val browser: JBCefBrowser

    init {
        val isOSREnabled = ServiceManager.getService(DevscodeExtensionSettings::class.java).devscodeState.enableOSR
        this.browser = JBCefBrowser.createBuilder().setOffScreenRendering(isOSREnabled).build()


        browser.jbCefClient.setProperty(
            JBCefClient.Properties.JS_QUERY_POOL_SIZE,
            JS_QUERY_POOL_SIZE
        )

        registerAppSchemeHandler()
        browser.loadURL(url);
        Disposer.register(DevscodePluginDisposable.getInstance(project), browser)

        // Listen for events sent from browser
        val myJSQueryOpenInBrowser = JBCefJSQuery.create((browser as JBCefBrowserBase?)!!)
        myJSQueryOpenInBrowser.addHandler { msg: String? ->
            val parser = JsonParser()
            val json: JsonObject = parser.parse(msg).asJsonObject
            val messageType = json.get("messageType").asString
            val data = json.get("data")
            val messageId = json.get("messageId")?.asString

            val devscodePluginService = ServiceManager.getService(
                project,
                DevscodePluginService::class.java
            )

            val ide = devscodePluginService.ideProtocolClient;

            val respond = fun(data: Any?) {
                // This matches the way that we expect receive messages in IdeMessenger.ts (gui)
                // and the way they are sent in VS Code (webviewProtocol.ts)
                var result: Map<String, Any?>? = null
                if (MessageTypes.generatorTypes.contains(messageType)) {
                    result = data as? Map<String, Any?>
                } else {
                    result = mutableMapOf(
                        "status" to "success",
                        "done" to false,
                        "content" to data
                    )
                }

                sendToWebview(messageType, result, messageId ?: uuid())
            }

            if (PASS_THROUGH_TO_CORE.contains(messageType)) {
                devscodePluginService.coreMessenger?.request(messageType, data, messageId, respond)
                return@addHandler null
            }

            when (messageType) {
                "jetbrains/editorInsetHeight" -> {
                    val height = data.asJsonObject.get("height").asInt
                    heightChangeListeners.forEach { it(height) }
                }

                "jetbrains/isOSREnabled" -> {
                    sendToWebview( "jetbrains/isOSREnabled", isOSREnabled)
                }

                "onLoad" -> {
                    coroutineScope.launch {
                        // Set the colors to match Intellij theme
                        val colors = GetTheme().getTheme();
                        sendToWebview("setColors", colors)

                        val jsonData = mutableMapOf(
                            "windowId" to devscodePluginService.windowId,
                            "workspacePaths" to devscodePluginService.workspacePaths,
                            "vscMachineId" to getMachineUniqueID(),
                            "vscMediaUrl" to "http://devscode",
                        )
                        respond(jsonData)
                    }

                }

                "showLines" -> {
                    val data = data.asJsonObject
                    ide?.setFileOpen(data.get("filepath").asString)
                    ide?.highlightCode(
                        RangeInFile(
                            data.get("filepath").asString,
                            Range(
                                Position(
                                    data.get("start").asInt,
                                    0
                                ), Position(
                                    data.get("end").asInt,
                                    0
                                )
                            ),

                            ), "#00ff0022"
                    )
                }

                "showTutorial" -> {
                    showTutorial(project)
                }

                "showVirtualFile" -> {
                    val data = data.asJsonObject
                    ide?.showVirtualFile(data.get("name").asString, data.get("content").asString)
                }

                "showFile" -> {
                    val data = data.asJsonObject
                    ide?.setFileOpen(data.get("filepath").asString)
                }

                "reloadWindow" -> {}

                "readRangeInFile" -> {
                    val data = data.asJsonObject
                    ide?.readRangeInFile(
                        RangeInFile(
                            data.get("filepath").asString,
                            Range(
                                Position(
                                    data.get("start").asInt,
                                    0
                                ), Position(
                                    data.get("end").asInt + 1,
                                    0
                                )
                            ),
                        )
                    )
                }

                "focusEditor" -> {}

                // IDE //
                else -> {
                    if (msg != null) {
                        ide?.handleMessage(msg, respond)
                    }
                }
            }


            null
        }

        // Listen for the page load event
        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadingStateChange(
                browser: CefBrowser?,
                isLoading: Boolean,
                canGoBack: Boolean,
                canGoForward: Boolean
            ) {
                if (!isLoading) {
                    // The page has finished loading
                    executeJavaScript(browser, myJSQueryOpenInBrowser)
                }
            }
        }, browser.cefBrowser)

    }

    fun executeJavaScript(browser: CefBrowser?, myJSQueryOpenInBrowser: JBCefJSQuery) {
        // Execute JavaScript - you might want to handle potential exceptions here
        val script = """window.postIntellijMessage = function(messageType, data, messageId) {
                const msg = JSON.stringify({messageType, data, messageId});
                ${myJSQueryOpenInBrowser.inject("msg")}
            }""".trimIndent()

        browser?.executeJavaScript(script, browser.url, 0)
    }

    fun sendToWebview(
        messageType: String,
        data: Any?,
        messageId: String = uuid()
    ) {
        val jsonData = Gson().toJson(
            mapOf(
                "messageId" to messageId,
                "messageType" to messageType,
                "data" to data
            )
        )
        val jsCode = buildJavaScript(jsonData)

        try {
            this.browser.executeJavaScriptAsync(jsCode)
        } catch (error: IllegalStateException) {
            println("Webview not initialized yet $error")
        }
    }

    private fun buildJavaScript(jsonData: String): String {
        return """window.postMessage($jsonData, "*");"""
    }

}