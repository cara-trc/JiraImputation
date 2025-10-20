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

    private val supportField = JTextField(JiraSettings.supportCard)
    private val runMgmtField = JTextField(JiraSettings.runManagement)

    init {
        title = "Configurer Jira Settings"; init()
    }

    override fun createCenterPanel(): JComponent {
        fun row(label: String, comp: JComponent) = JPanel(BorderLayout(8, 0)).apply {
            maximumSize = Dimension(520, 40)
            add(JLabel(label), BorderLayout.WEST); add(comp, BorderLayout.CENTER)
        }
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(row("Email Jira :", emailField)); add(Box.createVerticalStrut(10))
            add(row("API Token  :", tokenField)); add(Box.createVerticalStrut(10))
            add(row("Base URL   :", urlField)); add(Box.createVerticalStrut(16))
            add(row("Support card :", supportField)); add(Box.createVerticalStrut(8))
            add(row("Run management :", runMgmtField))
        }
    }

    override fun doValidate(): ValidationInfo? {
        val email = emailField.text.trim()
        val token = String(tokenField.password).trim()
        val base = urlField.text.trim()
        val keyRe = Regex("^[A-Z][A-Z0-9_]+-\\d+$") // ex: JIR-123, PROJ_ERP-42

        return when {
            !email.contains("@") -> ValidationInfo("Email invalide", emailField)
            token.isEmpty() -> ValidationInfo("Le token ne peut pas être vide", tokenField)
            !base.startsWith("http") -> ValidationInfo("Base URL doit commencer par http(s)://", urlField)
            supportField.text.isNotBlank() && !keyRe.matches(supportField.text.trim()) ->
                ValidationInfo("Format attendu: PROJ-123", supportField)

            runMgmtField.text.isNotBlank() && !keyRe.matches(runMgmtField.text.trim()) ->
                ValidationInfo("Format attendu: PROJ-123", runMgmtField)

            else -> null
        }
    }

    override fun doOKAction() {
        JiraSettings.update(
            emailField.text.trim(),
            String(tokenField.password).trim(),
            urlField.text.trim()
        )
        JiraSettings.updateSpecials(
            supportField.text.trim().ifBlank { "JIR-4" },
            runMgmtField.text.trim().ifBlank { "JIR-5" }
        )
        super.doOKAction()
    }
}
