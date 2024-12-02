package com.github.khulnasoft.devscodeintellijextension.services

import com.github.khulnasoft.devscodeintellijextension.constants.getConfigJsPath
import com.github.khulnasoft.devscodeintellijextension.constants.getConfigJsonPath
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.messages.Topic
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.File
import java.io.IOException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.*

class DevscodeSettingsComponent : DumbAware {
    val panel: JPanel = JPanel(GridBagLayout())
    val remoteConfigServerUrl: JTextField = JTextField()
    val remoteConfigSyncPeriod: JTextField = JTextField()
    val userToken: JTextField = JTextField()
    val enableTabAutocomplete: JCheckBox = JCheckBox("Enable Tab Autocomplete")
    val enableDevscodeTeamsBeta: JCheckBox = JCheckBox("Enable Devscode for Teams Beta")
    val enableOSR: JCheckBox = JCheckBox("Enable Off-Screen Rendering")
    val displayEditorTooltip: JCheckBox = JCheckBox("Display Editor Tooltip")

    init {
        val constraints = GridBagConstraints()

        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        constraints.weighty = 0.0
        constraints.gridx = 0
        constraints.gridy = GridBagConstraints.RELATIVE

        panel.add(JLabel("Remote Config Server URL:"), constraints)
        constraints.gridy++
        constraints.gridy++
        panel.add(remoteConfigServerUrl, constraints)
        constraints.gridy++
        panel.add(JLabel("Remote Config Sync Period (in minutes):"), constraints)
        constraints.gridy++
        panel.add(remoteConfigSyncPeriod, constraints)
        constraints.gridy++
        panel.add(JLabel("User Token:"), constraints)
        constraints.gridy++
        panel.add(userToken, constraints)
        constraints.gridy++
        panel.add(enableTabAutocomplete, constraints)
        constraints.gridy++
        panel.add(enableDevscodeTeamsBeta, constraints)
        constraints.gridy++
        panel.add(enableOSR, constraints)
        constraints.gridy++
        panel.add(displayEditorTooltip, constraints)
        constraints.gridy++

        // Add a "filler" component that takes up all remaining vertical space
        constraints.weighty = 1.0
        val filler = JPanel()
        panel.add(filler, constraints)
    }
}

@Serializable
class DevscodeRemoteConfigSyncResponse {
    var configJson: String? = null
    var configJs: String? = null
}

@State(
    name = "com.github.khulnasoft.devscodeintellijextension.services.DevscodeExtensionSettings",
    storages = [Storage("DevscodeExtensionSettings.xml")]
)
open class DevscodeExtensionSettings : PersistentStateComponent<DevscodeExtensionSettings.DevscodeState> {

    class DevscodeState {
        var lastSelectedInlineEditModel: String? = null
        var shownWelcomeDialog: Boolean = false
        var remoteConfigServerUrl: String? = null
        var remoteConfigSyncPeriod: Int = 60
        var userToken: String? = null
        var enableTabAutocomplete: Boolean = true
        var ghAuthToken: String? = null
        var enableDevscodeTeamsBeta: Boolean = false
        var enableOSR: Boolean = shouldRenderOffScreen()
        var displayEditorTooltip: Boolean = true
    }

    var devscodeState: DevscodeState = DevscodeState()

    private var remoteSyncFuture: ScheduledFuture<*>? = null

    override fun getState(): DevscodeState {
        return devscodeState
    }

    override fun loadState(state: DevscodeState) {
        devscodeState = state
    }

    companion object {
        val instance: DevscodeExtensionSettings
            get() = ServiceManager.getService(DevscodeExtensionSettings::class.java)
    }


    // Sync remote config from server
    private fun syncRemoteConfig() {
        val state = instance.devscodeState

        if (state.remoteConfigServerUrl != null && state.remoteConfigServerUrl!!.isNotEmpty()) {
            // download remote config as json file

            val client = OkHttpClient()
            val baseUrl = state.remoteConfigServerUrl?.removeSuffix("/")

            val requestBuilder = Request.Builder().url("${baseUrl}/sync")

            if (state.userToken != null) {
                requestBuilder.addHeader("Authorization", "Bearer ${state.userToken}")
            }

            val request = requestBuilder.build()
            var configResponse: DevscodeRemoteConfigSyncResponse? = null

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    response.body?.string()?.let { responseBody ->
                        try {
                            configResponse =
                                Json.decodeFromString<DevscodeRemoteConfigSyncResponse>(responseBody)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            return
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return
            }

            if (configResponse?.configJson?.isNotEmpty()!!) {
                val file = File(getConfigJsonPath(request.url.host))
                file.writeText(configResponse!!.configJson!!)
            }

            if (configResponse?.configJs?.isNotEmpty()!!) {
                val file = File(getConfigJsPath(request.url.host))
                file.writeText(configResponse!!.configJs!!)
            }
        }
    }

    // Create a scheduled task to sync remote config every `remoteConfigSyncPeriod` minutes
    fun addRemoteSyncJob() {

        if (remoteSyncFuture != null) {
            remoteSyncFuture?.cancel(false)
        }

        instance.remoteSyncFuture = AppExecutorUtil.getAppScheduledExecutorService()
            .scheduleWithFixedDelay(
                { syncRemoteConfig() },
                0,
                devscodeState.remoteConfigSyncPeriod.toLong(),
                TimeUnit.MINUTES
            )
    }
}

interface SettingsListener {
    fun settingsUpdated(settings: DevscodeExtensionSettings.DevscodeState)

    companion object {
        val TOPIC = Topic.create("SettingsUpdate", SettingsListener::class.java)
    }
}

class DevscodeExtensionConfigurable : Configurable {
    private var mySettingsComponent: DevscodeSettingsComponent? = null

    override fun createComponent(): JComponent {
        mySettingsComponent = DevscodeSettingsComponent()
        return mySettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings = DevscodeExtensionSettings.instance
        val modified =
            mySettingsComponent?.remoteConfigServerUrl?.text != settings.devscodeState.remoteConfigServerUrl ||
                    mySettingsComponent?.remoteConfigSyncPeriod?.text?.toInt() != settings.devscodeState.remoteConfigSyncPeriod ||
                    mySettingsComponent?.userToken?.text != settings.devscodeState.userToken ||
                    mySettingsComponent?.enableTabAutocomplete?.isSelected != settings.devscodeState.enableTabAutocomplete ||
                    mySettingsComponent?.enableDevscodeTeamsBeta?.isSelected != settings.devscodeState.enableDevscodeTeamsBeta ||
                    mySettingsComponent?.enableOSR?.isSelected != settings.devscodeState.enableOSR ||
                    mySettingsComponent?.displayEditorTooltip?.isSelected != settings.devscodeState.displayEditorTooltip
        return modified
    }

    override fun apply() {
        val settings = DevscodeExtensionSettings.instance
        settings.devscodeState.remoteConfigServerUrl = mySettingsComponent?.remoteConfigServerUrl?.text
        settings.devscodeState.remoteConfigSyncPeriod = mySettingsComponent?.remoteConfigSyncPeriod?.text?.toInt() ?: 60
        settings.devscodeState.userToken = mySettingsComponent?.userToken?.text
        settings.devscodeState.enableTabAutocomplete = mySettingsComponent?.enableTabAutocomplete?.isSelected ?: false
        settings.devscodeState.enableDevscodeTeamsBeta =
            mySettingsComponent?.enableDevscodeTeamsBeta?.isSelected ?: false
        settings.devscodeState.enableOSR = mySettingsComponent?.enableOSR?.isSelected ?: true
        settings.devscodeState.displayEditorTooltip = mySettingsComponent?.displayEditorTooltip?.isSelected ?: true

        ApplicationManager.getApplication().messageBus.syncPublisher(SettingsListener.TOPIC)
            .settingsUpdated(settings.devscodeState)
        DevscodeExtensionSettings.instance.addRemoteSyncJob()
    }

    override fun reset() {
        val settings = DevscodeExtensionSettings.instance
        mySettingsComponent?.remoteConfigServerUrl?.text = settings.devscodeState.remoteConfigServerUrl
        mySettingsComponent?.remoteConfigSyncPeriod?.text = settings.devscodeState.remoteConfigSyncPeriod.toString()
        mySettingsComponent?.userToken?.text = settings.devscodeState.userToken
        mySettingsComponent?.enableTabAutocomplete?.isSelected = settings.devscodeState.enableTabAutocomplete
        mySettingsComponent?.enableDevscodeTeamsBeta?.isSelected = settings.devscodeState.enableDevscodeTeamsBeta
        mySettingsComponent?.enableOSR?.isSelected = settings.devscodeState.enableOSR
        mySettingsComponent?.displayEditorTooltip?.isSelected = settings.devscodeState.displayEditorTooltip

        DevscodeExtensionSettings.instance.addRemoteSyncJob()
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }

    override fun getDisplayName(): String {
        return "Devscode Extension Settings"
    }
}

/**
 * This function checks if off-screen rendering (OSR) should be used.
 *
 * If ui.useOSR is set in config.json, that value is used.
 *
 * Otherwise, we check if the pluginSinceBuild is greater than or equal to 233, which corresponds
 * to IntelliJ platform version 2023.3 and later.
 *
 * Setting `setOffScreenRendering` to `false` causes a number of issues such as a white screen flash when loading
 * the GUI and the inability to set `cursor: pointer`. However, setting `setOffScreenRendering` to `true` on
 * platform versions prior to 2023.3.4 causes larger issues such as an inability to type input for certain languages,
 * e.g. Korean.
 *
 * References:
 * 1. https://youtrack.jetbrains.com/issue/IDEA-347828/JCEF-white-flash-when-tool-window-show#focus=Comments-27-9334070.0-0
 *    This issue mentions that white screen flash problems were resolved in platformVersion 2023.3.4.
 * 2. https://www.jetbrains.com/idea/download/other.html
 *    This documentation shows mappings from platformVersion to branchNumber.
 *
 * We use the branchNumber (e.g., 233) instead of the full version number (e.g., 2023.3.4) because
 * it's a simple integer without dot notation, making it easier to compare.
 */
private fun shouldRenderOffScreen(): Boolean {
    // With the 0.0.77 release, non-Mac users have been reporting issues with paste functionality
    // in the browser. Disabling OSR for all non-Mac users for now.
    if (!SystemInfo.isMac) {
        return false
    }

    val minBuildNumber = 233
    val applicationInfo = ApplicationInfo.getInstance()
    val currentBuildNumber = applicationInfo.build.baselineVersion
    return currentBuildNumber >= minBuildNumber
}