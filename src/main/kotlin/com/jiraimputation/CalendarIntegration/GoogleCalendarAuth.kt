package com.jiraimputation.CalendarIntegration

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import java.io.File
import java.io.FileReader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class GoogleCalendarAuth {

    private val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

    private fun getCredential(): Credential {
        val userHome = System.getProperty("user.home")
        val trackerDir = File(userHome, ".jira-tracker")
        val credentialsFile = File(trackerDir, "credentials.json")
        val secrets = GoogleClientSecrets.load(jsonFactory, FileReader(credentialsFile))

        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, secrets, listOf("https://www.googleapis.com/auth/calendar.readonly")
        )
            .setAccessType("offline")
            .setDataStoreFactory(FileDataStoreFactory(File("tokens")))
            .build()

        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    val service: Calendar by lazy {
        Calendar.Builder(httpTransport, jsonFactory, getCredential())
            .setApplicationName("JiraImputation Calendar Integration")
            .build()
    }
}