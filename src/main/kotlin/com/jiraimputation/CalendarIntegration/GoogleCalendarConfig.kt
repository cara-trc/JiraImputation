package com.jiraimputation.CalendarIntegration

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object GoogleCalendarConfig {
    private const val DEFAULT_BROKER = "https://broker-l4kpo7wk7q-ew.a.run.app"

    val SESSION_FILE: Path = Paths.get(
        System.getProperty("user.home"),
        ".jira-tracker",
        "session.key"
    )

    fun brokerUrl(): String =
        System.getenv("G_CAL_BROKER_URL")?.trim()
            ?: DEFAULT_BROKER

    fun sessionJwt(): String =
        loadSessionFromFile()
            ?: error("❌ Aucune clé trouvée. Lance l’action 'Login Google Calendar' pour te connecter.")

    private fun loadSessionFromFile(): String? =
        if (Files.exists(SESSION_FILE))
            Files.readString(SESSION_FILE, StandardCharsets.UTF_8).trim().ifEmpty { null }
        else null

}
