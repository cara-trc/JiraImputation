package com.jiraimputation.CalendarIntegration

import com.jiraimputation.models.WorklogBlock
import java.time.ZoneId
import kotlinx.datetime.toJavaInstant
import com.google.api.services.calendar.model.Event
import kotlin.time.Duration.Companion.seconds


class MeetingIntegrator(val googleCalendarClient: GoogleCalendarClient = GoogleCalendarClient()) {

    fun integrateMeetings(worklogBlocks: List<WorklogBlock>): List<WorklogBlock> {
        return worklogBlocks
            .groupBy { it.start.toJavaInstant().atZone(ZoneId.systemDefault()).toLocalDate() }
            .flatMap { (date, blocks) ->
                val meetings = googleCalendarClient.getEventsFor(date)
                    .mapNotNull { event: Event -> event.toWorklogBlockOrNull() }

                insertMeetingsInBlocks(blocks, meetings)
            }
    }


    fun insertMeetingsInBlocks(
        workBlocks: List<WorklogBlock>,
        meetings: List<WorklogBlock>
    ): List<WorklogBlock> {
        if (meetings.isEmpty()) return workBlocks

        var currentBlocks = workBlocks

        val result = meetings
            .sortedBy { it.start }
            .fold(mutableListOf<WorklogBlock>()) { acc, meeting ->
                val meetingStart = meeting.start
                val meetingEnd = meeting.start + meeting.durationSeconds.seconds

                currentBlocks = currentBlocks.flatMap { block ->
                    val blockStart = block.start
                    val blockEnd = block.start + block.durationSeconds.seconds

                    when {
                        // Block fully inside meeting => remove
                        blockStart >= meetingStart && blockEnd <= meetingEnd -> emptyList()

                        // Block overlaps start of meeting
                        blockStart < meetingStart && blockEnd > meetingStart && blockEnd <= meetingEnd -> listOf(
                            block.copy(durationSeconds = (meetingStart - blockStart).inWholeSeconds.toInt())
                        )

                        // Block overlaps end of meeting
                        blockStart < meetingEnd && blockEnd > meetingEnd && blockStart >= meetingStart -> listOf(
                            block.copy(
                                start = meetingEnd,
                                durationSeconds = (blockEnd - meetingEnd).inWholeSeconds.toInt()
                            )
                        )

                        // Block surrounds meeting => split
                        blockStart < meetingStart && blockEnd > meetingEnd -> listOf(
                            block.copy(durationSeconds = (meetingStart - blockStart).inWholeSeconds.toInt()),
                            block.copy(
                                start = meetingEnd,
                                durationSeconds = (blockEnd - meetingEnd).inWholeSeconds.toInt()
                            )
                        )

                        // No overlap
                        else -> listOf(block)
                    }
                }

                acc.apply { add(meeting) }
            }

        return (result + currentBlocks).sortedBy { it.start }
    }
}