package com.jotapem.dlmstranslator.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.*
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jotapem.dlmstranslator.MyBundle
import com.jotapem.dlmstranslator.services.DlmsToolWindowController
import com.jotapem.dlmstranslator.services.DlmsTranslatorService
import com.jotapem.dlmstranslator.services.TranslationHistoryEntry
import com.jotapem.dlmstranslator.services.TranslationHistoryManager
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.swing.*

class DlmsToolWindowFactory : ToolWindowFactory {

    private data class PanelResult(
        val component: JBPanel<*>,
        val inputArea: JBTextArea,
        val inputTypeCombo: ComboBox<DlmsTranslatorService.InputType>,
        val outputArea: EditorTextField,
        val performTranslation: () -> Unit
    )

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val historyManager = project.service<TranslationHistoryManager>()
        val historyListModel = DefaultListModel<TranslationHistoryEntry>()

        val result = createTranslationPanel(
            project,
            MyBundle.message("input.pdu.emptyText"),
            MyBundle.message("button.translatePdu.text"),
            historyManager,
            historyListModel
        ) { input, useHex, inputType -> DlmsTranslatorService.translateToResult(input, useHex, inputType) }

        project.service<DlmsToolWindowController>().apply {
            inputArea = result.inputArea
            inputTypeCombo = result.inputTypeCombo
            performTranslation = result.performTranslation
        }

        val mainPanel = JPanel(CardLayout())
        val cardLayout = mainPanel.layout as CardLayout
        val showingHistory = booleanArrayOf(false)

        val historyPanel = createHistoryPanel(
            historyManager = historyManager,
            historyListModel = historyListModel,
            inputArea = result.inputArea,
            inputTypeCombo = result.inputTypeCombo,
            outputArea = result.outputArea,
            onLoad = {
                showingHistory[0] = false
                cardLayout.show(mainPanel, "translation")
            }
        )

        mainPanel.add(result.component, "translation")
        mainPanel.add(historyPanel, "history")
        cardLayout.show(mainPanel, "translation")

        val toggleHistoryAction = object : ToggleAction(
            "Translation History",
            "Show/hide translation history",
            AllIcons.Vcs.History
        ), DumbAware {
            override fun isSelected(e: AnActionEvent) = showingHistory[0]

            override fun setSelected(e: AnActionEvent, state: Boolean) {
                showingHistory[0] = state
                cardLayout.show(mainPanel, if (state) "history" else "translation")
            }
        }

        toolWindow.setTitleActions(listOf(toggleHistoryAction))

        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createTranslationPanel(
        project: Project,
        inputEmptyText: String,
        buttonText: String,
        historyManager: TranslationHistoryManager,
        historyListModel: DefaultListModel<TranslationHistoryEntry>,
        translateAction: (String, Boolean, DlmsTranslatorService.InputType) -> DlmsTranslatorService.TranslationResult
    ): PanelResult {
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
                editor.setVerticalScrollbarVisible(true)
                editor.setHorizontalScrollbarVisible(true)
                editor.settings.isUseSoftWraps = false
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

        val performTranslation: () -> Unit = {
            if (inputArea.text.isNotBlank()) {
                val inputType = inputTypeCombo.selectedItem as DlmsTranslatorService.InputType
                when (val result = translateAction(inputArea.text, hexCheckBox.isSelected, inputType)) {
                    is DlmsTranslatorService.TranslationResult.Success -> {
                        outputArea.text = result.xml
                        val entry = TranslationHistoryEntry(
                            id = UUID.randomUUID().toString(),
                            timestampMillis = System.currentTimeMillis(),
                            input = inputArea.text,
                            inputType = inputType,
                            output = result.xml
                        )
                        historyManager.addEntry(entry)
                        historyListModel.add(0, entry)
                    }
                    is DlmsTranslatorService.TranslationResult.Error -> {
                        outputArea.text = result.message
                    }
                }
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

                val rightPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.RIGHT, 0, 0))

                add(leftPanel, BorderLayout.WEST)
                add(rightPanel, BorderLayout.EAST)
            }
            add(innerPanel, BorderLayout.CENTER)
        }

        panel.add(splitter, BorderLayout.CENTER)
        panel.add(bottomPanel, BorderLayout.SOUTH)

        return PanelResult(panel, inputArea, inputTypeCombo, outputArea, performTranslation)
    }

    private fun createHistoryPanel(
        historyManager: TranslationHistoryManager,
        historyListModel: DefaultListModel<TranslationHistoryEntry>,
        inputArea: JBTextArea,
        inputTypeCombo: ComboBox<DlmsTranslatorService.InputType>,
        outputArea: EditorTextField,
        onLoad: () -> Unit
    ): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())

        val headerPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty(8, 12, 4, 12)
            add(
                JBLabel(
                    MyBundle.message("history.header.label"),
                    UIUtil.ComponentStyle.SMALL,
                    UIUtil.FontColor.BRIGHTER
                ),
                BorderLayout.WEST
            )
            val clearBtn = JButton(MyBundle.message("history.clear.text")).apply {
                toolTipText = MyBundle.message("history.clear.description")
                putClientProperty("JButton.buttonType", "borderless")
                addActionListener {
                    historyManager.clearAll()
                    historyListModel.clear()
                }
            }
            add(clearBtn, BorderLayout.EAST)
        }

        val historyList = JBList(historyListModel).apply {
            emptyText.text = MyBundle.message("history.emptyText")
            cellRenderer = HistoryEntryRenderer()
            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }

        historyList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val index = historyList.locationToIndex(e.point)
                if (index < 0) return

                if (SwingUtilities.isLeftMouseButton(e)) {
                    val entry = historyListModel.getElementAt(index)
                    inputArea.text = entry.input
                    inputTypeCombo.selectedItem = entry.inputType
                    outputArea.text = entry.output
                    onLoad()
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    historyList.selectedIndex = index
                    showContextMenu(historyList, historyManager, historyListModel, index, e.x, e.y)
                }
            }
        })

        val scrollPane = JBScrollPane(historyList).apply {
            border = JBUI.Borders.empty()
        }

        panel.add(headerPanel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    private fun showContextMenu(
        historyList: JBList<TranslationHistoryEntry>,
        historyManager: TranslationHistoryManager,
        historyListModel: DefaultListModel<TranslationHistoryEntry>,
        index: Int,
        x: Int,
        y: Int
    ) {
        val entry = historyListModel.getElementAt(index)
        val popup = JPopupMenu()

        popup.add(JMenuItem(MyBundle.message("history.copyInput.text")).apply {
            toolTipText = MyBundle.message("history.copyInput.description")
            addActionListener {
                CopyPasteManager.getInstance().setContents(StringSelection(entry.input))
            }
        })

        popup.add(JMenuItem(MyBundle.message("history.copyOutput.text")).apply {
            toolTipText = MyBundle.message("history.copyOutput.description")
            addActionListener {
                CopyPasteManager.getInstance().setContents(StringSelection(entry.output))
            }
        })

        popup.addSeparator()

        popup.add(JMenuItem(MyBundle.message("history.delete.text")).apply {
            toolTipText = MyBundle.message("history.delete.description")
            addActionListener {
                historyManager.removeEntry(entry.id)
                historyListModel.remove(index)
            }
        })

        popup.show(historyList, x, y)
    }

    private class HistoryEntryRenderer : ListCellRenderer<TranslationHistoryEntry> {

        private val panel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty(5, 12)
        }
        private val leftPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            isOpaque = false
        }
        private val badgeLabel = JBLabel()
        private val previewLabel = JBLabel()
        private val timestampLabel = JBLabel()

        init {
            leftPanel.add(badgeLabel)
            leftPanel.add(previewLabel)
            panel.add(leftPanel, BorderLayout.WEST)
            panel.add(timestampLabel, BorderLayout.EAST)
        }

        override fun getListCellRendererComponent(
            list: JList<out TranslationHistoryEntry>,
            value: TranslationHistoryEntry,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val bg = UIUtil.getListBackground(isSelected, cellHasFocus)
            val fg = UIUtil.getListForeground(isSelected, cellHasFocus)

            panel.background = bg
            leftPanel.background = bg

            val inputTypeStr = when (value.inputType) {
                DlmsTranslatorService.InputType.HEX -> MyBundle.message("history.inputType.hex")
                DlmsTranslatorService.InputType.BASE64 -> MyBundle.message("history.inputType.base64")
            }
            badgeLabel.text = "[$inputTypeStr]"
            badgeLabel.foreground = UIUtil.getContextHelpForeground()

            val preview = if (value.input.length > 40) value.input.take(40) + "..." else value.input
            previewLabel.text = preview
            previewLabel.foreground = fg

            timestampLabel.text = formatTimestamp(value.timestampMillis)
            timestampLabel.foreground = UIUtil.getContextHelpForeground()

            return panel
        }

        private fun formatTimestamp(millis: Long): String {
            val entryDate = Date(millis)
            val calEntry = Calendar.getInstance().apply { time = entryDate }
            val calNow = Calendar.getInstance()
            return if (calEntry.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calEntry.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
            ) {
                SimpleDateFormat("HH:mm:ss").format(entryDate)
            } else {
                SimpleDateFormat("dd/MM/yyyy HH:mm").format(entryDate)
            }
        }
    }
}
