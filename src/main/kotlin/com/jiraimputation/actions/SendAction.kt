package com.jiraimputation.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.jiraimputation.LunchInserter.LunchUserPreference
import com.jiraimputation.SpecialTreatment.TransformSpecialLogs
import com.jiraimputation.aggregator.WorklogAggregator
import com.jiraimputation.models.LogEntry
import com.jiraimputation.sender.WorklogSender
import kotlinx.serialization.json.Json
import java.io.File

class WorklogAggregateSendAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        try {
            LunchUserPreference.forceLaunch = PropertiesComponent.getInstance().getBoolean("LunchBreakForce", false)

            val userHome = System.getProperty("user.home")
            val logFile = File(userHome, ".jira-tracker/worklog.json")

            val aggregator = WorklogAggregator()
            val postTreatment = TransformSpecialLogs()
            val logs = logFile.readLines()
                .filter { it.isNotBlank() }
                .mapIndexedNotNull { index, line ->
                    runCatching {
                        Json.decodeFromString(LogEntry.serializer(), line)
                    }.getOrElse {
                        println("⚠️ Ligne $index invalide : ${it.message}")
                        null
                    }
                }
            val blocks = aggregator.aggregateLogsToWorklogBlocks(logs)

            val modifiedBlocks = postTreatment.replaceSpecialIssueKeys(blocks)
            WorklogSender.sendAll(modifiedBlocks)

            Notifications.Bus.notify(
                Notification("JiraImputation Notifications", "Succès", "${blocks.size} blocs imputés.", NotificationType.INFORMATION)
            )
        } catch (ex: Exception) {
            Notifications.Bus.notify(
                Notification("JiraImputation Notifications", "Erreur", "Échec : ${ex.message}", NotificationType.ERROR)
            )
        }
    }
}
