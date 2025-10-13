package com.jiraimputation.sender

import com.jiraimputation.Secrets
import com.jiraimputation.models.WorklogBlock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.File
import java.time.*
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
        @Body request: WorklogRequest
    ): Call<Void>
}

// --------- Sender Singleton ---------
object WorklogSender {

    private const val EMAIL = Secrets.email
    private const val API_TOKEN = Secrets.jiraToken
    private const val BASE_URL = Secrets.baseUrl

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", Credentials.basic(EMAIL, API_TOKEN))
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val api: JiraApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(JiraApi::class.java)

    fun sendAll(blocks: List<WorklogBlock>) {
        val userHome = System.getProperty("user.home")
        val trackerDir = File(userHome, ".jira-tracker")
        val debugFile = File(trackerDir, "sender.log")
        val logFile = File(trackerDir, "worklog.json")

        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        debugFile.appendText("start sendAll\n")

        blocks.forEach { block ->
            val zone = ZoneId.of("Europe/Paris")
            val javaInstant = Instant.ofEpochSecond(block.start.epochSeconds)
            val offset = javaInstant.atZone(zone).toOffsetDateTime()

            val formatted = offset.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))

            val request = WorklogRequest(
                started = formatted,
                timeSpentSeconds = block.durationSeconds
            )

            val response = api.postWorklog(block.issueKey, request).execute()

            val logLine = if (response.isSuccessful) {
                "[$now] ✅ ${block.issueKey} à $formatted (${block.durationSeconds}s)"
            } else {
                "[$now] ❌ ${block.issueKey} à $formatted — HTTP ${response.code()}: ${response.errorBody()?.string()}"
            }

            debugFile.appendText(logLine + "\n")
            if (response.isSuccessful) {
                //logs sent successfully, reset the json
                logFile.writeText("")
            }
        }
    }


}
