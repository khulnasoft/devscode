package com.github.khulnasoft.devscodeintellijextension.autocomplete

import com.github.khulnasoft.devscodeintellijextension.services.DevscodeExtensionSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service

class AutocompleteActionGroup : DefaultActionGroup() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        removeAll()

        val devscodeSettingsService = service<DevscodeExtensionSettings>()
        if (devscodeSettingsService.devscodeState.enableTabAutocomplete) {
            addAll(
                DisableTabAutocompleteAction(),
            )
        } else {
            addAll(
                EnableTabAutocompleteAction(),
            )
        }
    }
}