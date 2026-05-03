package com.jotapem.dlmstranslator.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextArea

@Service(Service.Level.PROJECT)
class DlmsToolWindowController {
    var inputArea: JBTextArea? = null
    var inputTypeCombo: ComboBox<DlmsTranslatorService.InputType>? = null
    var performTranslation: (() -> Unit)? = null
    var suppressAutoTranslation: Boolean = false

    fun translateInputFromEditor(frame: String) {
        suppressAutoTranslation = true
        inputArea?.text = frame
        inputTypeCombo?.selectedItem = DlmsTranslatorService.InputType.HEX
        suppressAutoTranslation = false
        performTranslation?.invoke()
    }
}
