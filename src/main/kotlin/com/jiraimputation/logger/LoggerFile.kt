package com.jiraimputation.logger

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.jiraimputation.models.LogEntry
import git4idea.GitUtil
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service(Service.Level.APP)
class LoggerFile {
    private val json = Json { prettyPrint = false }
    private val userHome = System.getProperty("user.home")
    private val trackerDir = File(userHome, ".jira-tracker")
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    val debugFile = File(trackerDir, "debug.log")

    init {
        val app = ApplicationManager.getApplication()
        Disposer.register(app) {
            scheduler.shutdownNow()
            debugFile.appendText("[${nowForLog()}] ⛔ Loggerfile arrêté\n")
        }
        println("→ Loggerfile APP Service démarré")
        debugFile.appendText("[${nowForLog()}] ✅ Loggerfile démarré\n")
        startLogging()
    }

    private fun startLogging() {
        val logFile = File(trackerDir, "worklog.json")

        trackerDir.mkdirs()
        if (!logFile.exists()) logFile.createNewFile()

        scheduler.scheduleAtFixedRate({
            val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return@scheduleAtFixedRate
            val repo = GitUtil.getRepositoryManager(project).repositories.firstOrNull() ?: return@scheduleAtFixedRate

            if (LoggerState.trackingPaused) return@scheduleAtFixedRate

            val fullBranch = repo.currentBranchName ?: return@scheduleAtFixedRate
            val issueKey = fullBranch.substringAfterLast("/")

            try {
                val newTimestamp = Clock.System.now().toString()
                val entry = LogEntry.BranchLog(branch = issueKey, timestamp = newTimestamp)
                val line = json.encodeToString(LogEntry.serializer(), entry)
                logFile.appendText("$line\n")

                println("→ Branch logged : $issueKey")
                debugFile.appendText("[${nowForLog()}] Log: $issueKey\n")
            } catch (e: Exception) {
                debugFile.appendText("[${nowForLog()}] Error : ${e.message}\n")
            }
        }, 0, 5, TimeUnit.MINUTES)
    }

    private fun nowForLog(): String = LocalDateTime.now().toString()
}
