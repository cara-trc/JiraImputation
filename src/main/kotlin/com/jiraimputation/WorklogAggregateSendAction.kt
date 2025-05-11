package com.jiraimputation

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.jiraimputation.aggregator.WorklogAggregator
import com.jiraimputation.models.BranchLog
import com.jiraimputation.sender.WorklogSender
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File

class WorklogAggregateSendAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        try {

            val aggregator = WorklogAggregator()
            val fakeLogPath = "C:\\Users\\DSPCv\\.jira-tracker\\worklog.json"
            val content = File(fakeLogPath).readText()

            val logs = Json.decodeFromString<List<BranchLog>>(content)
            val blocks = aggregator.aggregateLogsToWorklogBlocks(logs)

            WorklogSender.sendAll(blocks)

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
