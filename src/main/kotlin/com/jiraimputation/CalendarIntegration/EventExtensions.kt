package com.jiraimputation.CalendarIntegration

import com.google.api.services.calendar.model.Event
import com.jiraimputation.SpecialTreatment.SpecialsTasks
import com.jiraimputation.models.WorklogBlock
import kotlinx.datetime.Instant

fun Event.toWorklogBlockOrNull(): WorklogBlock? {
    val startDateTime = this.start.dateTime
    val endDateTime = this.end.dateTime

    if (startDateTime == null || endDateTime == null) {
        println("⏭️ Skip all-day or incomplete event: ${this.summary}")
        return null
    }

    val startInstant = Instant.fromEpochMilliseconds(startDateTime.value)
    val endInstant = Instant.fromEpochMilliseconds(endDateTime.value)
    val duration = (endInstant - startInstant).inWholeSeconds.toInt()

    return WorklogBlock(
        issueKey = SpecialsTasks.meetings,
        start = startInstant,
        durationSeconds = duration
    )
}