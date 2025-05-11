package com.jiraimputation.logger


import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import git4idea.GitUtil

class LogBranchAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repo = GitUtil.getRepositoryManager(project).repositories.firstOrNull() ?: return
        val branch = repo.currentBranchName ?: return
        val regex = Regex("([A-Z]+-\\d+)$")
        val shortenedBranch = regex.find(branch)?.value
        NotificationGroupManager.getInstance()
            .getNotificationGroup("JiraImputation Notifications")
            .createNotification("Branche actuelle : $shortenedBranch", NotificationType.INFORMATION)
            .notify(project)
    }
}
