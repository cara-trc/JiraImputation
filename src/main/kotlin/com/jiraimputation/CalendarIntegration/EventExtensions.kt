package com.jiraimputation.CalendarIntegration

import com.google.api.services.calendar.model.Event
import com.jiraimputation.models.WorklogBlock
import kotlinx.datetime.Instant
import org.jsoup.Jsoup

fun Event.toWorklogBlockOrNull(): WorklogBlock? {
    val startDateTime = this.start.dateTime
    val endDateTime = this.end.dateTime

    if (startDateTime == null || endDateTime == null) {
        println("⏭️ Skip all-day or incomplete event: ${this.summary}")
        return null
    }
    if (!this.isAttendedByMe()) {
        println("⏭️ Skip event not attended by me: ${this.summary}")
        return null
    }

    val startInstant = Instant.fromEpochMilliseconds(startDateTime.value)
    val endInstant = Instant.fromEpochMilliseconds(endDateTime.value)
    val duration = (endInstant - startInstant).inWholeSeconds.toInt()

    val descriptionText = this.description?.let { Jsoup.parse(it).text() } ?: return null

    val issueKeyFromDescription = Regex("""\[(.+?)]""")
        .find(descriptionText)
        ?.groupValues
        ?.get(1)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: return null

    return WorklogBlock(
        issueKey = issueKeyFromDescription,
        start = startInstant,
        durationSeconds = duration
    )
}

fun Event.isAttendedByMe(): Boolean {
    if (organizer?.self == true) {
        return true
    }
    val attendee = attendees?.find { it.self == true }
    return attendee?.responseStatus == "accepted"
}