package com.github.khulnasoft.devscodeintellijextension.`devscode`

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.JComponent
import javax.swing.JTextArea

class InputBoxInlayProvider : InlayHintsProvider<NoSettings> {
    override val key: SettingsKey<NoSettings>
        get() = SettingsKey<NoSettings>("InputBoxInlayProviderSettingsKey")
    override val name: String
        get() = "Devscode Quick Input"

    override val previewText: String?
        get() = "Devscode Quick Input"

    override fun createSettings() = NoSettings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        return Collector(editor)
    }

    private class Collector(editor: Editor) : FactoryInlayHintsCollector(editor) {
        override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
            if (element.text == "devscode") {
//                val presentation = HorizontalBarPresentation.create(factory, editor, element)
//                sink.addInlineElement(element.textOffset, true, presentation)
            }
            return true
        }
    }

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return JTextArea("Hello World!")
            }
        }
    }
}