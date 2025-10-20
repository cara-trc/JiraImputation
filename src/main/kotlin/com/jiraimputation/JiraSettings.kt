package com.jiraimputation

import com.intellij.openapi.application.ApplicationManager

object JiraSettings {
    private val svc get() = ApplicationManager.getApplication().getService(JiraSettingsService::class.java)

    val email get() = svc.email()
    val jiraToken get() = svc.jiraToken()
    val baseUrl get() = svc.baseUrl()
    val supportCard get() = svc.supportCard()
    val runManagement get() = svc.runManagement()

    fun update(email: String, token: String, baseUrl: String) = svc.update(email, token, baseUrl)
    fun updateSpecials(supportCard: String, runManagement: String) = svc.updateSpecials(supportCard, runManagement)
}
