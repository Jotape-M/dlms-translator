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
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.openapi.ui.ComboBox
import com.jotapem.dlmstranslator.MyBundle
import com.jotapem.dlmstranslator.services.DlmsTranslatorService
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import javax.swing.*

class DlmsToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val mainPanel = createTranslationPanel(
            project,
            MyBundle.message("input.pdu.emptyText"),
            MyBundle.message("button.translatePdu.text")
        ) { input, useHex, inputType -> DlmsTranslatorService.translate(input, useHex, inputType) }

        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createTranslationPanel(
        project: Project,
        inputEmptyText: String,
        buttonText: String,
        translateAction: (String, Boolean, DlmsTranslatorService.InputType) -> String
    ): JBPanel<*> {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())

        // --- 1. ÁREA DE ENTRADA (HEXADECIMAL) ---
        val inputArea = JBTextArea().apply {
            emptyText.text = inputEmptyText
            lineWrap = true
            wrapStyleWord = true
            margin = JBUI.insets(8)
            background = UIUtil.getTextFieldBackground()
            font = EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)

        }

        val inputTypeCombo = ComboBox(DlmsTranslatorService.InputType.entries.toTypedArray()).apply {
            renderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                    if (value is DlmsTranslatorService.InputType) {
                        label.text = when (value) {
                            DlmsTranslatorService.InputType.HEX -> MyBundle.message("options.inputType.hex")
                            DlmsTranslatorService.InputType.BASE64 -> MyBundle.message("options.inputType.base64")
                        }
                    }
                    return label
                }
            }
        }

        val inputHeader = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty(0, 12, 4, 12)
            val leftHeaderPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                isOpaque = false
                add(
                    JBLabel(MyBundle.message("input.header.label"), UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER)
                )
                add(Box.createHorizontalStrut(8))
                add(inputTypeCombo)
            }
            add(leftHeaderPanel, BorderLayout.WEST)
        }

        val inputContainer = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty(8, 0, 4, 0)
            add(inputHeader, BorderLayout.NORTH)
            val scrollPaneContainer = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.empty(0, 12)
                add(JBScrollPane(inputArea), BorderLayout.CENTER)
            }
            add(scrollPaneContainer, BorderLayout.CENTER)
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

        val outputHeader = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 12)
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

        val outputContainer = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.merge(
                IdeBorderFactory.createBorder(SideBorder.TOP),
                JBUI.Borders.empty(4, 0, 8, 0),
                true
            )
            add(outputHeader, BorderLayout.NORTH)
            val outputAreaContainer = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.empty(0, 12)
                add(outputArea, BorderLayout.CENTER)
            }
            add(outputAreaContainer, BorderLayout.CENTER)
        }

        // --- 3. DIVISOR CENTRAL ---
        val splitter = JBSplitter(true).apply {
            firstComponent = inputContainer
            secondComponent = outputContainer
            dividerWidth = 1
            border = JBUI.Borders.empty()
        }

        val hexCheckBox = JBCheckBox(MyBundle.message("options.showHex.text"), true)

        fun performTranslation() {
            if (inputArea.text.isNotBlank()) {
                val inputType = inputTypeCombo.selectedItem as DlmsTranslatorService.InputType
                val result = translateAction(inputArea.text, hexCheckBox.isSelected, inputType)
                outputArea.text = result
            }
        }

        val translateBtn = JButton(buttonText).apply {
            putClientProperty("JButton.buttonType", "defaultButton")
            icon = AllIcons.Actions.Compile

            addActionListener {
                performTranslation()
            }
        }

        hexCheckBox.addActionListener { performTranslation() }
        inputTypeCombo.addActionListener { performTranslation() }

        val bottomPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 0)

            val innerPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.empty(0, 10)
                val leftPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                    add(translateBtn)
                    add(Box.createHorizontalStrut(8))
                    add(hexCheckBox)
                }

                val rightPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
                }

                add(leftPanel, BorderLayout.WEST)
                add(rightPanel, BorderLayout.EAST)
            }
            add(innerPanel, BorderLayout.CENTER)
        }

        panel.add(splitter, BorderLayout.CENTER)
        panel.add(bottomPanel, BorderLayout.SOUTH)

        return panel
    }
}