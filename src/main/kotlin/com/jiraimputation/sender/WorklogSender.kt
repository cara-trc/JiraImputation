package com.jiraimputation.sender

import com.jiraimputation.JiraSettings
import com.jiraimputation.models.WorklogBlock
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// --------- Request DTO ---------
data class WorklogRequest(
    val started: String,
    val timeSpentSeconds: Int,
    val comment: Map<String, Any> = mapOf(
        "type" to "doc",
        "version" to 1,
        "content" to listOf(
            mapOf(
                "type" to "paragraph",
                "content" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to "Imputé automatiquement depuis le plugin JiraImputation"
                    )
                )
            )
        )
    ),
)

// --------- Retrofit API ---------
interface JiraApi {
    @POST("rest/api/3/issue/{issueKey}/worklog")
    fun postWorklog(
        @Path("issueKey") issueKey: String,
        @Body body: WorklogRequest
    ): Call<Unit>

    @GET("rest/api/3/myself")
    fun myself(): Call<Unit>
}

// --------- Sender ---------
object WorklogSender {

    // --- Utils ---
    private fun normalizeBaseUrl(raw: String): String {
        val v = raw.trim()
        if (v.isEmpty()) return v
        val origin = v.replace(Regex("^(https?://[^/]+).*$"), "$1")
        return if (origin.endsWith("/")) origin else "$origin/"
    }

    private fun buildApi(): JiraApi {
        val email = JiraSettings.email.trim()
        val token = JiraSettings.jiraToken.trim()
        val baseUrl = normalizeBaseUrl(JiraSettings.baseUrl)

        require(email.isNotBlank()) { "Email Jira manquant" }
        require(token.isNotBlank()) { "API token Jira manquant" }
        require(baseUrl.startsWith("https://") && baseUrl.endsWith("/")) {
            "Base URL Jira invalide"
        }

        val basic = Credentials.basic(email, token, Charsets.UTF_8)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Authorization", basic)
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(req)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl) // DOIT finir par '/'
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(JiraApi::class.java)
    }

    private fun assertAuth(api: JiraApi) {
        val r: Response<Unit> = api.myself().execute()
        require(r.isSuccessful) {
            "Auth Jira KO — HTTP ${r.code()}: ${r.errorBody()?.string() ?: ""}"
        }
    }

    fun sendAll(blocks: List<WorklogBlock>) {
        val api = buildApi() // lit les settings actuels à chaque envoi

        // Folders/logs
        val userHome = System.getProperty("user.home")
        val trackerDir = File(userHome, ".jira-tracker").apply { mkdirs() }
        val debugFile = File(trackerDir, "sender.log")
        val logFile = File(trackerDir, "worklog.json")

        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        debugFile.appendText("start sendAll\n")

        // Self-test avant d'envoyer quoi que ce soit (fail fast)
        try {
            assertAuth(api)
        } catch (e: Throwable) {
            debugFile.appendText("[$now] ❌ Auth Jira KO: ${e.message}\n")
            throw e
        }

        val paris = ZoneId.of("Europe/Paris")
        val startedFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

        blocks.forEach { block ->
            val javaInstant = Instant.ofEpochSecond(block.start.epochSeconds)
            val started = javaInstant.atZone(paris).toOffsetDateTime().format(startedFmt)

            val request = WorklogRequest(
                started = started,
                timeSpentSeconds = block.durationSeconds
            )

            val response = api.postWorklog(block.issueKey, request).execute()

            val logLine = if (response.isSuccessful) {
                "[$now] ✅ ${block.issueKey} à $started (${block.durationSeconds}s)"
            } else {
                val err = try { response.errorBody()?.string() } catch (_: Throwable) { null }
                "[$now] ❌ ${block.issueKey} à $started — HTTP ${response.code()}: ${err ?: ""}"
            }

            debugFile.appendText(logLine + "\n")
            if (response.isSuccessful) {
                // logs sent successfully, reset the json
                runCatching { logFile.writeText("") }
            }
        }
    }
}
