package com.jiraimputation


import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service
@State(name = "JiraSettingsState", storages = [Storage("JiraImputation.xml")])
class JiraSettingsService : PersistentStateComponent<JiraSettingsState> {
    private var state: JiraSettingsState = JiraSettingsState()


    override fun getState(): JiraSettingsState = state
    override fun loadState(state: JiraSettingsState) { XmlSerializerUtil.copyBean(state, this.state) }


    fun email(): String = state.email?.trim().orEmpty()
    fun jiraToken(): String = state.jiraToken?.trim().orEmpty()
    fun baseUrl(): String = normalizeBaseUrl(state.baseUrl)


    fun supportCard(): String = state.supportCard?.trim().orEmpty()
    fun runManagement(): String = state.runManagement?.trim().orEmpty()


    fun update(email: String, token: String, baseUrl: String) {
        state.email = email.trim()
        state.jiraToken = token.trim()
        state.baseUrl = normalizeBaseUrl(baseUrl)
    }


    fun updateSpecials(supportCard: String, runManagement: String) {
        state.supportCard = supportCard.trim()
        state.runManagement = runManagement.trim()
    }


    private fun normalizeBaseUrl(raw: String?): String {
        val v = raw?.trim().orEmpty()
        if (v.isEmpty()) return v
        val origin = v.replace(Regex("^(https?://[^/]+).*$"), "$1")
        return if (origin.endsWith("/")) origin else "$origin/"
    }
}

class JiraSettingsState {
    var email: String? = null
    var jiraToken: String? = null
    var baseUrl: String? = null
    var supportCard: String? = null
    var runManagement: String? = null
}

private fun String?.orElseDefault(def: String) =
    this?.trim().takeUnless { it.isNullOrEmpty() } ?: def
