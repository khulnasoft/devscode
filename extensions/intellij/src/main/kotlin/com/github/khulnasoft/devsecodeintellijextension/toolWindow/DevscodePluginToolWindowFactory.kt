package com.github.khulnasoft.devscodeintellijextension.toolWindow

import com.github.khulnasoft.devscodeintellijextension.services.DevscodePluginService
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.*

const val JS_QUERY_POOL_SIZE = "200"

class DevscodePluginToolWindowFactory : ToolWindowFactory, DumbAware {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val devscodeToolWindow = DevscodePluginWindow(project)
    val content =
        ContentFactory.getInstance().createContent(devscodeToolWindow.content, null, false)
    toolWindow.contentManager.addContent(content)
    val titleActions = mutableListOf<AnAction>()
    createTitleActions(titleActions)

    // Add MaximizeToolWindow action
    val action = ActionManager.getInstance().getAction("MaximizeToolWindow")
    if (action != null) {
      titleActions.add(action)
    }

    toolWindow.setTitleActions(titleActions)
  }

  private fun createTitleActions(titleActions: MutableList<in AnAction>) {
    val action = ActionManager.getInstance().getAction("DevscodeSidebarActionsGroup")
    if (action != null) {
      titleActions.add(action)
    }
  }

  override fun shouldBeAvailable(project: Project) = true

  class DevscodePluginWindow(project: Project) {
    private val defaultGUIUrl = "http://devscode/index.html"

    init {
      System.setProperty("ide.browser.jcef.jsQueryPoolSize", JS_QUERY_POOL_SIZE)
      System.setProperty("ide.browser.jcef.contextMenu.devTools.enabled", "true")
    }

    val browser: DevscodeBrowser by lazy {
      val url = System.getenv("GUI_URL")?.toString() ?: defaultGUIUrl

      val browser = DevscodeBrowser(project, url)
      val devscodePluginService =
          ServiceManager.getService(project, DevscodePluginService::class.java)
      devscodePluginService.devscodePluginWindow = this
      browser
    }

    val content: JComponent
      get() = browser.browser.component
  }
}
