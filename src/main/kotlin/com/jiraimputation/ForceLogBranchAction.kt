package com.jiraimputation

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import git4idea.GitUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime

class ForceLogBranchAction : AnAction("Forcer log branche maintenant"), DumbAware {

    private val json = Json { prettyPrint = true }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repo = GitUtil.getRepositoryManager(project).repositories.firstOrNull()

        if (repo == null) {
            Messages.showWarningDialog(project, "Aucun dépôt Git détecté.", "Erreur")
            return
        }

        val branch = repo.currentBranchName
        if (branch == null) {
            Messages.showWarningDialog(project, "Impossible de détecter la branche actuelle.", "Erreur")
            return
        }

        try {
            val userHome = System.getProperty("user.home")
            val trackerDir = File(userHome, ".jira-tracker")
            val logFile = File(trackerDir, "worklog.json")

            trackerDir.mkdirs()
            if (!logFile.exists()) logFile.writeText("[]")

            val existing = try {
                json.decodeFromString<List<BranchLog>>(logFile.readText())
            } catch (e: Exception) {
                emptyList()
            }

            val newEntry = BranchLog(LocalDateTime.now().toString(), branch)
            val updated = existing + newEntry
            logFile.writeText(json.encodeToString(updated))

            Messages.showInfoMessage(project, "Branche logguée : $branch", "Succès")

        } catch (ex: Exception) {
            Messages.showErrorDialog(project, "Erreur : ${ex.message}", "Erreur")
            ex.printStackTrace()
        }
    }
}
