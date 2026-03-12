package com.jotapem.dlmstranslator.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.*
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jotapem.dlmstranslator.MyBundle
import com.jotapem.dlmstranslator.services.DlmsTranslatorService
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel

class DlmsToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        // Criamos apenas o painel de PDU
        val mainPanel = createTranslationPanel(
            project,
            MyBundle.message("input.pdu.emptyText"),
            MyBundle.message("button.translatePdu.text")
        ) { input, useHex -> DlmsTranslatorService.translate(input, useHex) }

        // Registamos como o conteúdo único da janela.
        // Como passamos "" (vazio) no título, o IntelliJ não vai criar a barra de abas!
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createTranslationPanel(
        project: Project,
        inputEmptyText: String,
        buttonText: String,
        translateAction: (String, Boolean) -> String
    ): JPanel {
        val panel = JPanel(BorderLayout())

        // --- 1. ÁREA DE ENTRADA (HEXADECIMAL) ---
        val inputArea = JBTextArea().apply {
            emptyText.text = inputEmptyText
            lineWrap = true
            wrapStyleWord = true
            margin = JBUI.insets(8)
            background = UIUtil.getTextFieldBackground()
            font = EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)

        }

        val inputHeader = JPanel(BorderLayout()).apply {
            add(
                JBLabel(MyBundle.message("input.header.label"), UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER),
                BorderLayout.WEST
            )

            val clearAction = object : DumbAwareAction(
                MyBundle.message("input.clear.text"),
                MyBundle.message("input.clear.description"),
                AllIcons.Actions.GC
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    inputArea.text = ""
                }
            }

            val actionGroup = DefaultActionGroup().apply {
                add(clearAction)
            }

            val toolbar = ActionManager.getInstance().createActionToolbar("DLMSInputToolbar", actionGroup, true).apply {
                targetComponent = inputArea
                component.isOpaque = false
                component.border = JBUI.Borders.empty()
            }
            add(toolbar.component, BorderLayout.EAST)
        }

        val inputContainer = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8, 12, 4, 12)
            add(inputHeader, BorderLayout.NORTH)
            add(JBScrollPane(inputArea), BorderLayout.CENTER)
        }

        // --- 2. ÁREA DE SAÍDA (XML) ---
        val xmlFileType = FileTypeManager.getInstance().getFileTypeByExtension("xml")
        val outputArea = EditorTextField(project, xmlFileType).apply {
            isViewer = true
            setOneLineMode(false)
            setFontInheritedFromLAF(false)
            addSettingsProvider { editor ->
                editor.setBorder(BorderFactory.createLineBorder(JBColor.border(), 1, true))
                editor.settings.isLineNumbersShown = true
                editor.settings.isFoldingOutlineShown = true
            }
        }

        val outputHeader = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 0)
            add(
                JBLabel(
                    MyBundle.message("output.header.label"),
                    UIUtil.ComponentStyle.SMALL,
                    UIUtil.FontColor.BRIGHTER
                ), BorderLayout.WEST
            )

            val copyAction = object : DumbAwareAction(
                MyBundle.message("output.copy.text"),
                MyBundle.message("output.copy.description"),
                AllIcons.Actions.Copy
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    val text = outputArea.text
                    if (text.isNotEmpty()) {
                        CopyPasteManager.getInstance().setContents(StringSelection(text))
                    }
                }
            }

            val actionGroup = DefaultActionGroup().apply {
                add(copyAction)
            }

            val toolbar =
                ActionManager.getInstance().createActionToolbar("DLMSOutputToolbar", actionGroup, true).apply {
                    targetComponent = outputArea
                    component.isOpaque = false
                    component.border = JBUI.Borders.empty()
                }
            add(toolbar.component, BorderLayout.EAST)
        }

        val outputContainer = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                IdeBorderFactory.createBorder(SideBorder.TOP),
                JBUI.Borders.empty(4, 12, 8, 12)
            )
            add(outputHeader, BorderLayout.NORTH)
            add(outputArea, BorderLayout.CENTER)
        }

        // --- 3. DIVISOR CENTRAL ---
        val splitter = JBSplitter(true).apply {
            firstComponent = inputContainer
            secondComponent = outputContainer
        }

        // --- 4. BARRA INFERIOR DE BOTÕES ---
        val hexCheckBox = JBCheckBox(MyBundle.message("options.showHex.text"), true)

        val translateBtn = JButton(buttonText).apply {
            putClientProperty("JButton.buttonType", "defaultButton")
            icon = AllIcons.Actions.Compile

            addActionListener {
                if (inputArea.text.isNotBlank()) {
                    val result = translateAction(inputArea.text, hexCheckBox.isSelected)
                    outputArea.text = result
                }
            }
        }

        hexCheckBox.addActionListener {
            if (inputArea.text.isNotBlank()) {
                val result = translateAction(inputArea.text, hexCheckBox.isSelected)
                outputArea.text = result
            }
        }

        val bottomPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            border = BorderFactory.createCompoundBorder(
                IdeBorderFactory.createBorder(SideBorder.TOP),
                JBUI.Borders.empty(10)
            )
            add(translateBtn)
            add(javax.swing.Box.createHorizontalStrut(12))
            add(hexCheckBox)
        }

        panel.add(splitter, BorderLayout.CENTER)
        panel.add(bottomPanel, BorderLayout.SOUTH)

        return panel
    }
}