package com.jiraimputation

import com.intellij.openapi.application.ApplicationManager

object JiraSettings {
    private val svc get() = ApplicationManager.getApplication().getService(JiraSettingsService::class.java)

    val email get() = svc.email()
    val jiraToken get() = svc.jiraToken()
    val baseUrl get() = svc.baseUrl()

    fun update(email: String, token: String, baseUrl: String) = svc.update(email.trim(), token.trim(), baseUrl.trim())
}
