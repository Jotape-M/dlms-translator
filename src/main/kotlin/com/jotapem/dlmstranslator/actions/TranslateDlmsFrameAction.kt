package com.jotapem.dlmstranslator.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.jotapem.dlmstranslator.MyBundle
import com.jotapem.dlmstranslator.services.DlmsFrameNormalizer
import com.jotapem.dlmstranslator.services.DlmsToolWindowController

class TranslateDlmsFrameAction : DumbAwareAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val hasSelection = e.getData(CommonDataKeys.EDITOR)
            ?.selectionModel?.hasSelection() == true
        e.presentation.isEnabledAndVisible = e.project != null && hasSelection
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selected = editor.selectionModel.selectedText

        if (selected.isNullOrBlank()) {
            notify(project, MyBundle.message("error.editor.noSelection"))
            return
        }

        when (val result = DlmsFrameNormalizer.normalize(selected)) {
            is DlmsFrameNormalizer.Result.Error -> {
                val message = when (result.type) {
                    DlmsFrameNormalizer.Result.ErrorType.INVALID_CONTENT ->
                        MyBundle.message("error.editor.invalidHexFrame")
                    DlmsFrameNormalizer.Result.ErrorType.ODD_LENGTH ->
                        MyBundle.message("error.editor.oddHexLength")
                }
                notify(project, message)
            }
            is DlmsFrameNormalizer.Result.Success -> {
                val toolWindow = ToolWindowManager.getInstance(project)
                    .getToolWindow("DLMS_Translator") ?: return
                toolWindow.show {
                    project.service<DlmsToolWindowController>()
                        .translateInputFromEditor(result.normalized)
                }
            }
        }
    }

    private fun notify(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("DLMS Translator Notifications")
            .createNotification("DLMS Translator", message, NotificationType.WARNING)
            .notify(project)
    }
}
