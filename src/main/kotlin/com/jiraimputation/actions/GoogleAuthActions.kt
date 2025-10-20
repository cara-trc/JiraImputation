package com.jiraimputation.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.jiraimputation.CalendarIntegration.GoogleCalendarConfig
import java.awt.Desktop
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class GoogleAuthAction : AnAction("Login Google Calendar") {

    override fun actionPerformed(e: AnActionEvent) {
        val brokerUrl = GoogleCalendarConfig.brokerUrl().trimEnd('/')

        try {
            val authUrl = "$brokerUrl/auth/start?scopes=https://www.googleapis.com/auth/calendar.readonly"
            Desktop.getDesktop().browse(URI(authUrl))

            val jwt = Messages.showInputDialog(
                "Connexion Google réussie ?\n\nColle la clé affichée sur la page ici :",
                "Importer la clé Google Calendar",
                null
            )?.trim()

            if (jwt.isNullOrEmpty()) {
                Messages.showWarningDialog("Aucune clé saisie. Auth annulée.", "JiraImputation")
                return
            }

            val path = GoogleCalendarConfig.SESSION_FILE
            Files.createDirectories(path.parent)
            Files.writeString(path, jwt, StandardCharsets.UTF_8)

            Messages.showInfoMessage(
                "Clé enregistrée dans ${path.toAbsolutePath()}\nTu peux maintenant synchroniser ton calendrier.",
                "Connexion Google réussie"
            )

        } catch (ex: Exception) {
            Messages.showErrorDialog("Erreur pendant l'auth Google : ${ex.message}", "JiraImputation")
        }
    }
}
