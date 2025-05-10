package com.jiraimputation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import git4idea.GitUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
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
                debugFile.appendText("[${now()}] Scheduler arrêté\n")
            }

            scheduler.scheduleAtFixedRate({
                if (project.isDisposed) {
                    scheduler.shutdownNow()
                    return@scheduleAtFixedRate
                }

                val branch = repo.currentBranchName
                if (branch == null) {
                    debugFile.appendText("[${now()}] Branche null\n")
                    return@scheduleAtFixedRate
                }

                try {
                    val existing = try {
                        json.decodeFromString<List<BranchLog>>(logFile.readText())
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val updated = existing + BranchLog(now(), branch)
                    logFile.writeText(json.encodeToString(updated))

                    println("→ Branche logguée : $branch")
                    debugFile.appendText("[${now()}] Loggué: $branch\n")

                } catch (e: Exception) {
                    debugFile.appendText("[${now()}] Erreur : ${e.message}\n")
                    logger.error("Erreur JSON log", e)
                }

            }, 0, 1, TimeUnit.MINUTES)
        }
    }

    private fun now(): String = LocalDateTime.now().toString()
}
