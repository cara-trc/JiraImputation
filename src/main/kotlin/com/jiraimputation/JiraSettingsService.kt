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

    // --- PasswordSafe pour le token ---
    private val credAttrs = CredentialAttributes("JiraImputation:APIToken")

    fun email(): String = state.email.orEmpty()
    fun baseUrl(): String = state.baseUrl.orEmpty()

    fun jiraToken(): String {
        val creds = PasswordSafe.instance.get(credAttrs)
        return creds?.getPasswordAsString().orEmpty()
    }

    fun update(email: String, token: String, baseUrl: String) {
        state.email = email
        state.baseUrl = baseUrl
        PasswordSafe.instance.set(credAttrs, Credentials(email.ifBlank { "jira" }, token))
    }
}

data class JiraSettingsState(
    var email: String? = null,
    var baseUrl: String? = null
)
