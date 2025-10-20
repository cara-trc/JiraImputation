package com.jiraimputation


import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(
    name = "JiraImputationSettings",
    storages = [Storage("jira-imputation.xml")]
)
class JiraSettingsService : PersistentStateComponent<JiraSettingsState> {

    private var state = JiraSettingsState()

    override fun getState(): JiraSettingsState = state
    override fun loadState(state: JiraSettingsState) { this.state = state }

    fun email(): String = state.email.orEmpty()
    fun baseUrl(): String = state.baseUrl.orEmpty()
    fun jiraToken(): String {
        val attrs = CredentialAttributes("JiraImputation:APIToken")
        return PasswordSafe.instance.get(attrs)?.getPasswordAsString().orEmpty()
    }

    fun supportCard(): String = state.supportCard.orElseDefault("JIR-4")
    fun runManagement(): String = state.runManagement.orElseDefault("JIR-5")

    fun update(email: String, token: String, baseUrl: String) {
        state.email = email
        state.baseUrl = baseUrl
        PasswordSafe.instance.set(
            CredentialAttributes("JiraImputation:APIToken"),
            Credentials(email.ifBlank { "jira" }, token)
        )
    }

    fun updateSpecials(supportCard: String, runManagement: String) {
        state.supportCard = supportCard
        state.runManagement = runManagement
    }
}

data class JiraSettingsState(
    var email: String? = null,
    var baseUrl: String? = null,
    var supportCard: String? = null,
    var runManagement: String? = null
)

private fun String?.orElseDefault(def: String) =
    this?.trim().takeUnless { it.isNullOrEmpty() } ?: def
