package com.jiraimputation.CalendarIntegration

import com.google.api.services.calendar.model.Event
import com.google.api.client.util.DateTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit


class GoogleCalendarClient() {

    val googleCalendarAuth = GoogleCalendarAuth()

    private fun getEventsBetween(start: Instant, end: Instant): List<Event> {
        val events = googleCalendarAuth.service.events().list("primary")
            .setTimeMin(DateTime(start.toEpochMilli()))
            .setTimeMax(DateTime(end.toEpochMilli()))
            .setSingleEvents(true)
            .setOrderBy("startTime")
            .execute()

        return events.items
    }
    fun getEventsFor(date: LocalDate): List<Event> {
        val start = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val end = start.plus(1, ChronoUnit.DAYS)
        return getEventsBetween(start, end)
    }
}