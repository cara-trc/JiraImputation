package com.jiraimputation.CalendarIntegration

import com.google.api.services.calendar.model.Event
import com.jiraimputation.SpecialTreatment.SpecialsTasks
import com.jiraimputation.models.WorklogBlock
import kotlinx.datetime.Instant

fun Event.toWorklogBlock(): WorklogBlock {
    val startInstant = Instant.fromEpochMilliseconds(this.start.dateTime.value)
    val endInstant = Instant.fromEpochMilliseconds(this.end.dateTime.value)
    val duration = endInstant.minus(startInstant).inWholeSeconds.toInt()

    return WorklogBlock(
        issueKey = SpecialsTasks.meetings,
        start = startInstant,
        durationSeconds = duration
    )
}