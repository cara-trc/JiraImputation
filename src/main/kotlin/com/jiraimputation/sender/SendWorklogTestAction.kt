package com.jiraimputation.sender

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.jiraimputation.models.WorklogBlock

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

import kotlinx.datetime.*

class SendWorklogTestAction : AnAction("Test Jira Worklog Send"), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val zone = TimeZone.of("Europe/Paris")

        // Bloc unique : vendredi 10 mai 2025 de 14h à 15h
        val start = LocalDateTime(2025, 5, 9, 14, 0).toInstant(zone)

        val block = WorklogBlock(
            issueKey = "JIR-1",
            start = start,
            durationSeconds = 3600
        )

        WorklogSender.sendAll(listOf(block))
        notify("Worklog Test", "Bloc de 1h envoyé vers TES-2 (14h à 15h)")
    }



    private fun notify(title: String, content: String, type: NotificationType = NotificationType.INFORMATION) {
        val notification = Notification("JiraImputation Notifications", title, content, type)
        Notifications.Bus.notify(notification)
    }
}

