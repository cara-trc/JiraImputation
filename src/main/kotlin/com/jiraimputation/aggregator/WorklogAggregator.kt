package com.jiraimputation.aggregator

import com.jiraimputation.CalendarIntegration.GoogleCalendarClient
import com.jiraimputation.CalendarIntegration.toWorklogBlock
import com.jiraimputation.LunchInserter.LunchBreakManager
import com.jiraimputation.LunchInserter.LunchUserPreference
import com.jiraimputation.models.LogEntry
import com.jiraimputation.models.WorklogBlock
import kotlinx.datetime.Instant
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

const val CHUNK_SIZE = 3

class WorklogAggregator {

    val googleCalendarClient = GoogleCalendarClient()
    val lunchBreakManager = LunchBreakManager(LunchUserPreference.forceLaunch)

    fun splitSequences(logs: List<LogEntry>): List<List<LogEntry.BranchLog>> {
        val logsWithLunch = lunchBreakManager.applyIfNeeded(logs)
        return logsWithLunch
            .fold(mutableListOf(mutableListOf<LogEntry.BranchLog>())) { acc, entry ->
                when (entry) {
                    is LogEntry.BranchLog -> {
                        val current = acc.last()
                        val lastLog = current.lastOrNull()

                        if (
                            lastLog != null &&
                            lastLog.timestamp.substring(0, 10) != entry.timestamp.substring(0, 10)
                        ) {
                            // Changement de jour → nouvelle séquence
                            acc.add(mutableListOf(entry))
                        } else {
                            current.add(entry)
                        }
                    }

                    is LogEntry.PauseMarker -> if (acc.last().isNotEmpty()) {
                        acc.add(mutableListOf())
                    }
                }
                acc
            }
            .filter { it.isNotEmpty() }
    }

    fun aggregateSequence(sequence: List<LogEntry.BranchLog>): List<WorklogBlock> {
        return sequence
            .chunked(CHUNK_SIZE)
            .map { chunk ->
                val issueKey = if (chunk.size == CHUNK_SIZE) {
                    chunk.groupingBy { it.branch }
                        .eachCount()
                        .maxBy { it.value }.key
                } else {
                    chunk.first().branch
                }

                val start = Instant.parse(chunk.first().timestamp)
                val durationSeconds = chunk.size * 5 * 60

                WorklogBlock(issueKey, start, durationSeconds)
            }
    }

    fun mergeConsecutiveBlocks(blocks: List<WorklogBlock>): List<WorklogBlock> {
        return blocks.fold(mutableListOf()) { acc, block ->
            if (acc.isNotEmpty() && acc.last().issueKey == block.issueKey) {
                acc[acc.lastIndex] = acc.last().copy(
                    durationSeconds = acc.last().durationSeconds + block.durationSeconds
                )
            } else {
                acc.add(block)
            }
            acc
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
                        // Block full during the meeting => delete the block
                        blockStart >= meetingStart && blockEnd <= meetingEnd -> emptyList()

                        // Block starts before the meeting and end after the beginning of the meeting => block end date is now the meeting start date
                        blockStart < meetingStart && blockEnd > meetingStart && blockEnd <= meetingEnd -> listOf(
                            block.copy(durationSeconds = (meetingStart - blockStart).inWholeSeconds.toInt())
                        )

                        // Bloc starts during the meeting => block startDate is now meeting endDate
                        blockStart < meetingEnd && blockEnd > meetingEnd && blockStart >= meetingStart -> listOf(
                            block.copy(
                                start = meetingEnd,
                                durationSeconds = (blockEnd - meetingEnd).inWholeSeconds.toInt()
                            )
                        )

                        // Block starts before and end after the meeting => block is split
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



    fun aggregateLogsToWorklogBlocks(logs: List<LogEntry>): List<WorklogBlock> {
        return splitSequences(logs).flatMap { sequence ->
            val date = LocalDate.parse(sequence.first().timestamp.substring(0, 10))
            val meetings = googleCalendarClient.getEventsFor(date).map { it.toWorklogBlock() }

            val workBlocks = aggregateSequence(sequence)
            val mergedWork = mergeConsecutiveBlocks(workBlocks)

            insertMeetingsInBlocks(mergedWork, meetings)
        }
    }
}





