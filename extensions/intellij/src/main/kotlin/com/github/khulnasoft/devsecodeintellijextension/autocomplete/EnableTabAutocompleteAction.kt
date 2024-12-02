package com.github.khulnasoft.devscodeintellijextension.autocomplete

import com.github.khulnasoft.devscodeintellijextension.services.DevscodeExtensionSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
class EnableTabAutocompleteAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val devscodeSettingsService = service<DevscodeExtensionSettings>()
        devscodeSettingsService.devscodeState.enableTabAutocomplete = true
    }
}
