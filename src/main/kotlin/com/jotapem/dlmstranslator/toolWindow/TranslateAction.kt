package com.jotapem.dlmstranslator.toolWindow

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.jotapem.dlmstranslator.MyBundle
import com.jotapem.dlmstranslator.services.DlmsTranslatorService

class TranslateAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val selectedText = editor.selectionModel.selectedText

        val notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("DLMS Notifications")

        if (!selectedText.isNullOrBlank()) {
            val translation = DlmsTranslatorService.translate(selectedText)

            notificationGroup.createNotification(
                MyBundle.message("notification.translate.title"),
                translation,
                NotificationType.INFORMATION
            ).notify(project)
        } else {
            notificationGroup.createNotification(
                "DLMS Translator",
                MyBundle.message("notification.noSelection.message"),
                NotificationType.WARNING
            ).notify(project)
        }
    }
}