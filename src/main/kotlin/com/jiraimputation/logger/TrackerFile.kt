package com.jiraimputation.logger

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.jiraimputation.models.BranchLog
import git4idea.GitUtil
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TrackerPlugin : ProjectActivity {

    private val logger = Logger.getInstance("JiraImputation")
    private val json = Json { prettyPrint = true }

    override suspend fun execute(project: Project) {
        println("→ Plugin JiraImputation lancé pour ${project.name}")

        ApplicationManager.getApplication().invokeLater {
            val repo = GitUtil.getRepositoryManager(project).repositories.firstOrNull()

            if (repo == null) {
                println("→ Aucun repo Git détecté")
                return@invokeLater
            }

            val userHome = System.getProperty("user.home")
            val trackerDir = File(userHome, ".jira-tracker")
            val logFile = File(trackerDir, "worklog.json")
            val debugFile = File(trackerDir, "debug.log")

            trackerDir.mkdirs()
            if (!logFile.exists()) logFile.writeText("[]")

            val scheduler = Executors.newSingleThreadScheduledExecutor()

            Disposer.register(project) {
                scheduler.shutdownNow()
                println("→ Scheduler arrêté proprement")
                debugFile.appendText("[${nowForLog()}] Scheduler arrêté\n")
            }

            scheduler.scheduleAtFixedRate({
                if (project.isDisposed) {
                    scheduler.shutdownNow()
                    return@scheduleAtFixedRate
                }

                val fullBranch = repo.currentBranchName
                if (fullBranch == null) {
                    debugFile.appendText("[${nowForLog()}] Branche null\n")
                    return@scheduleAtFixedRate
                }

                // ✅ Extraction de l'issue key : "feature/PRJ-1" → "PRJ-1"
                val issueKey = if (fullBranch.contains("/")) {
                    fullBranch.substringAfterLast("/")
                } else {
                    fullBranch
                }

                try {
                    val existing = try {
                        json.decodeFromString<List<BranchLog>>(logFile.readText())
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val newTimestamp = Clock.System.now().toString()
                    val updated = existing + BranchLog(newTimestamp, issueKey)
                    logFile.writeText(json.encodeToString(updated))

                    println("→ Branche logguée : $issueKey")
                    debugFile.appendText("[${nowForLog()}] Loggué: $issueKey\n")

                } catch (e: Exception) {
                    debugFile.appendText("[${nowForLog()}] Erreur : ${e.message}\n")
                    logger.error("Erreur JSON log", e)
                }

            }, 0, 1, TimeUnit.MINUTES)
        }
    }

    // Horodatage local uniquement pour les logs de debug
    private fun nowForLog(): String = LocalDateTime.now().toString()
}
