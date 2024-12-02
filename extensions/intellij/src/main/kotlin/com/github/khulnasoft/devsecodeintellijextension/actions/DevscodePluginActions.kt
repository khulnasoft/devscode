package com.github.khulnasoft.devscodeintellijextension.actions

import com.github.khulnasoft.devscodeintellijextension.editor.DiffStreamService
import com.github.khulnasoft.devscodeintellijextension.services.DevscodePluginService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout

fun getPluginService(project: Project?): DevscodePluginService? {
    if (project == null) {
        return null
    }
    return ServiceManager.getService(
        project,
        DevscodePluginService::class.java
    )
}

class AcceptDiffAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        acceptHorizontalDiff(e)
        acceptVerticalDiff(e)
    }

    private fun acceptHorizontalDiff(e: AnActionEvent) {
        val devscodePluginService = getPluginService(e.project) ?: return
        devscodePluginService.ideProtocolClient?.diffManager?.acceptDiff(null)
    }

    private fun acceptVerticalDiff(e: AnActionEvent) {
        val project = e.project ?: return
        val editor =
            e.getData(PlatformDataKeys.EDITOR) ?: FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val diffStreamService = project.service<DiffStreamService>()
        diffStreamService.accept(editor)
    }
}

class RejectDiffAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        rejectHorizontalDiff(e)
        rejectVerticalDiff(e)
    }

    private fun rejectHorizontalDiff(e: AnActionEvent) {
        val devscodePluginService = getPluginService(e.project) ?: return
        devscodePluginService.ideProtocolClient?.diffManager?.rejectDiff(null)
    }

    private fun rejectVerticalDiff(e: AnActionEvent) {
        val project = e.project ?: return
        val editor =
            e.getData(PlatformDataKeys.EDITOR) ?: FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val diffStreamService = project.service<DiffStreamService>()
        diffStreamService.reject(editor)
    }
}

fun getDevscodePluginService(project: Project?): DevscodePluginService? {
    if (project != null) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("Devscode")

        if (toolWindow != null) {
            if (!toolWindow.isVisible) {
                toolWindow.activate(null)
            }
        }
    }

    return getPluginService(project)
}

fun focusDevscodeInput(project: Project?) {
    val devscodePluginService = getDevscodePluginService(project) ?: return
    devscodePluginService.devscodePluginWindow?.content?.components?.get(0)?.requestFocus()
    devscodePluginService.sendToWebview("focusDevscodeInputWithoutClear", null)

    devscodePluginService.ideProtocolClient?.sendHighlightedCode()
}

class FocusDevscodeInputWithoutClearAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        focusDevscodeInput(project)
    }
}

class FocusDevscodeInputAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val devscodePluginService = getDevscodePluginService(e.project) ?: return

        devscodePluginService.devscodePluginWindow?.content?.components?.get(0)?.requestFocus()
        devscodePluginService.sendToWebview("focusDevscodeInput", null)

        devscodePluginService.ideProtocolClient?.sendHighlightedCode()
    }
}

class NewDevscodeSessionAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val devscodePluginService = getDevscodePluginService(e.project) ?: return
        devscodePluginService.devscodePluginWindow?.content?.components?.get(0)?.requestFocus()
        devscodePluginService.sendToWebview("focusDevscodeInputWithNewSession", null)
    }
}

class ViewHistoryAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val devscodePluginService = getDevscodePluginService(e.project) ?: return
        devscodePluginService.sendToWebview("viewHistory", null)
    }
}