package com.jiraimputation.logger

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.jiraimputation.models.LogEntry
import git4idea.GitUtil
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TrackerPlugin : ProjectActivity {

    private val json = Json { prettyPrint = false }

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
            val logFile = File(trackerDir, "worklog.json") // .json pour JSON Lines
            val debugFile = File(trackerDir, "debug.log")

            trackerDir.mkdirs()
            if (!logFile.exists()) logFile.createNewFile()

            val scheduler = Executors.newSingleThreadScheduledExecutor()

            Disposer.register(project) {
                scheduler.shutdownNow()
                debugFile.appendText("[${nowForLog()}] Scheduler arrêté\n")
            }

            scheduler.scheduleAtFixedRate({
                if (project.isDisposed) {
                    scheduler.shutdownNow()
                    return@scheduleAtFixedRate
                }

                if (LoggerState.trackingPaused) {
                    return@scheduleAtFixedRate
                }

                val fullBranch = repo.currentBranchName
                if (fullBranch == null) {
                    debugFile.appendText("[${nowForLog()}] Branche null\n")
                    return@scheduleAtFixedRate
                }

                val issueKey = if (fullBranch.contains("/")) {
                    fullBranch.substringAfterLast("/")
                } else {
                    fullBranch
                }

                try {
                    val newTimestamp = Clock.System.now().toString()
                    val entry = LogEntry.BranchLog(branch = issueKey, timestamp = newTimestamp)
                    appendLog(logFile, entry)

                    println("→ Branch logged : $issueKey")
                    debugFile.appendText("[${nowForLog()}] Log: $issueKey\n")

                } catch (e: Exception) {
                    debugFile.appendText("[${nowForLog()}] Error : ${e.message}\n")
                }

            }, 0, 2, TimeUnit.MINUTES)
        }
    }

    private fun appendLog(file: File, entry: LogEntry) {
        val line = json.encodeToString(LogEntry.serializer(), entry)
        file.appendText("$line\n")
    }

    private fun nowForLog(): String = LocalDateTime.now().toString()
}
