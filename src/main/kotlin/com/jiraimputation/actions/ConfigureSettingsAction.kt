package com.jiraimputation.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.jiraimputation.JiraSettings
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class ConfigureJiraSettingsAction : AnAction("Configure Jira Settings") {
    override fun actionPerformed(e: AnActionEvent) {
        JiraSettingsDialog().showAndGet()
    }
}

private class JiraSettingsDialog : DialogWrapper(true) {

    private val emailField = JTextField(JiraSettings.email)
    private val tokenField = JPasswordField(JiraSettings.jiraToken)
    private val urlField = JTextField(JiraSettings.baseUrl.ifBlank { "https://xxxxx.atlassian.net" })

    init {
        title = "Configurer Jira Settings"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val form = JPanel()
        form.layout = BoxLayout(form, BoxLayout.Y_AXIS)

        fun row(label: String, comp: JComponent): JPanel {
            val p = JPanel(BorderLayout(8, 0))
            p.maximumSize = Dimension(500, 40)
            p.add(JLabel(label), BorderLayout.WEST)
            p.add(comp, BorderLayout.CENTER)
            return p
        }

        form.add(row("Email Jira :", emailField))
        form.add(Box.createVerticalStrut(10))
        form.add(row("API Token :", tokenField))
        form.add(Box.createVerticalStrut(10))
        form.add(row("Base URL Jira :", urlField))
        return form
    }

    override fun doValidate(): ValidationInfo? {
        val email = emailField.text.trim()
        val token = String(tokenField.password).trim()
        val base = urlField.text.trim()

        if (!email.contains("@")) return ValidationInfo("Email invalide", emailField)
        if (token.isEmpty()) return ValidationInfo("Le token ne peut pas être vide", tokenField)
        if (!base.startsWith("http")) return ValidationInfo("Base URL doit commencer par http(s)://", urlField)
        return null
    }

    override fun doOKAction() {
        val email = emailField.text.trim()
        val token = String(tokenField.password).trim()
        val base = urlField.text.trim()
        JiraSettings.update(email, token, base)
        super.doOKAction()
    }
}
