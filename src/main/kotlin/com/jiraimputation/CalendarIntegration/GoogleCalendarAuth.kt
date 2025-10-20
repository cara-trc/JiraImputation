package com.jiraimputation.CalendarIntegration

import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class GoogleCalendarAuth(
    applicationName: String = "JiraImputation"
) {
    val service: Calendar

    init {
        val brokerUrl = GoogleCalendarConfig.brokerUrl()
        val sessionJwt = GoogleCalendarConfig.sessionJwt()
        val initializer = BrokerRequestInitializer(brokerUrl, sessionJwt)
        service = Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            initializer
        ).setApplicationName(applicationName).build()
    }
}

/** Retrofit layer **/
data class TokenRequest(val session: String)
data class TokenResponse(val access_token: String, val expires_in: Long)

interface BrokerApi {
    @POST("/token")
    fun fetchToken(@Body body: TokenRequest): Call<TokenResponse>
}

/** Injecte un access_token via ton broker **/
private class BrokerRequestInitializer(
    private val serviceUrl: String,
    private val sessionJwt: String
) : HttpRequestInitializer {

    private val cachedToken = AtomicReference<String?>(null)
    private val expiresAtEpochSec = AtomicLong(0)

    private val api: BrokerApi = run {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logger).build()

        Retrofit.Builder()
            .baseUrl(serviceUrl.ensureEndsWithSlash())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BrokerApi::class.java)
    }

    override fun initialize(request: HttpRequest) {
        val now = Instant.now().epochSecond
        val token = if (cachedToken.get().isNullOrBlank() || now >= expiresAtEpochSec.get() - 5) {
            val fresh = fetchAccessToken()
            cachedToken.set(fresh.first)
            expiresAtEpochSec.set(now + fresh.second)
            fresh.first
        } else {
            cachedToken.get()!!
        }
        request.headers.authorization = "Bearer $token"
    }

    private fun fetchAccessToken(): Pair<String, Long> {
        val resp = api.fetchToken(TokenRequest(sessionJwt)).execute()
        require(resp.isSuccessful) {
            "Broker token error: ${resp.code()} ${resp.errorBody()?.string()}"
        }
        val body = resp.body() ?: error("Réponse vide du broker")
        return body.access_token to body.expires_in
    }

    private fun String.ensureEndsWithSlash(): String =
        if (endsWith("/")) this else "$this/"
}
