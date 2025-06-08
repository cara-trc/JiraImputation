package com.jiraimputation.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jiraimputation.CalendarIntegration.GoogleCalendarClient
import java.io.File


class GoogleCalendarTestAction : AnAction("Test Google Calendar") {

    override fun actionPerformed(e: AnActionEvent) {
        val userHome = System.getProperty("user.home")
        val trackerDir = File(userHome, ".jira-tracker")
        val debugFile = File(trackerDir, "debug.log")
        debugFile.parentFile.mkdirs()

        debugFile.appendText("==== Google Calendar Test ====\n")
        runCatching {
            val googleClient = GoogleCalendarClient()
            val events = googleClient.getTodayEvents()
            val filtered = events.filter { it.start.dateTime != null && it.end.dateTime != null }

            if (filtered.isEmpty()) {
                debugFile.appendText("❌ Aucun événement trouvé aujourd'hui\n")
            } else {
                debugFile.appendText("✅ Événements trouvés :\n")
                for (event in filtered) {
                    val start = event.start.dateTime!!
                    val end = event.end.dateTime!!
                    val durationMinutes = (end.value - start.value) / 60000

                    debugFile.appendText("➡️ ${event.summary} @ $start (${durationMinutes} min)\n")
                }
            }
        }.onFailure {
            debugFile.appendText("❌ Erreur pendant la lecture : ${it.message}\n")
        }
    }
}
