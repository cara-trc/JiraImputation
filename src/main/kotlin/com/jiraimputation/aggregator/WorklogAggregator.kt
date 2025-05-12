package com.jiraimputation.aggregator

import com.jiraimputation.models.BranchLog
import com.jiraimputation.models.WorklogBlock
import kotlinx.datetime.*
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

const val CHUNK_SIZE = 3

class WorklogAggregator {
    val chunkDuration = 15.minutes
    fun aggregateLogsToWorklogBlocks(parsedLogs: List<BranchLog>): List<WorklogBlock> {
        val grouped = mutableListOf<Triple<String, Instant, Int>>()

        val sortedLogs = parsedLogs.sortedBy { Instant.parse(it.timestamp) }
        // imputation starts on next multiple of 15 minutes so it is displayed correctly on calendar
        val firstValidStart = Instant.parse(sortedLogs.first().timestamp).ceilToQuarterHour()

        val logsToAggregate = sortedLogs.filter { Instant.parse(it.timestamp) >= firstValidStart }
        //main algo
        logsToAggregate.foldIndexed(Unit) { index, _, _ ->
            if (index + CHUNK_SIZE > parsedLogs.size) return@foldIndexed

            val chunk = parsedLogs.subList(index, index + CHUNK_SIZE)
            val branches = chunk.map { it.branch }
            val times = chunk.map { Instant.parse(it.timestamp) }

            val start = times.first().roundToQuarterHour()
            val end = times.last().plus(5.minutes)
            val expectedEnd = start + chunkDuration

            if (end <= expectedEnd) {
                val majorityBranch = branches.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key
                grouped.add(Triple(majorityBranch, start, chunkDuration.inWholeSeconds.toInt()))
            }
        }

        // If json ends with a chunk <3 (if i stop working at 18h20 for exemple), the next chunk is extended to the next multiple of 15min
        if (logsToAggregate.size % CHUNK_SIZE != 0) {
            val remainingStart = parsedLogs.size - (CHUNK_SIZE - 1)
            if (remainingStart in 0 until parsedLogs.size) {
                val remaining = parsedLogs.subList(remainingStart, parsedLogs.size)
                val branches = remaining.map { it.branch }
                val times = remaining.map { Instant.parse(it.timestamp) }

                val start = times.first().roundToQuarterHour()
                val majorityBranch = branches.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key

                grouped.add(Triple(majorityBranch, start, chunkDuration.inWholeSeconds.toInt()))
            }
        }

        return mergeConsecutiveBlocks(
            grouped.map { (branch, start, duration) ->
                WorklogBlock(issueKey = branch, start = start, durationSeconds = duration)
            }
        )
    }




    private fun Instant.roundToQuarterHour(): Instant {
        val zone = TimeZone.currentSystemDefault()
        val local = this.toLocalDateTime(zone)
        val roundedMinute = (local.minute / 15) * 15

        val rounded = LocalDateTime(
            year = local.year,
            monthNumber = local.monthNumber,
            dayOfMonth = local.dayOfMonth,
            hour = local.hour,
            minute = roundedMinute
        )

        return rounded.toInstant(zone)
    }
    private fun Instant.ceilToQuarterHour(): Instant {
        val zone = TimeZone.currentSystemDefault()
        val local = this.toLocalDateTime(zone)

        val roundedMinute = if (local.minute % 15 == 0) {
            local.minute
        } else {
            ((local.minute / 15) + 1) * 15
        }

        val adjusted = if (roundedMinute < 60) {
            LocalDateTime(
                year = local.year,
                monthNumber = local.monthNumber,
                dayOfMonth = local.dayOfMonth,
                hour = local.hour,
                minute = roundedMinute
            )
        } else {
            LocalDateTime(
                year = local.year,
                monthNumber = local.monthNumber,
                dayOfMonth = local.dayOfMonth,
                hour = local.hour + 1,
                minute = 0
            )
        }

        return adjusted.toInstant(zone)
    }


    private fun mergeConsecutiveBlocks(blocks: List<WorklogBlock>): List<WorklogBlock> {
        if (blocks.isEmpty()) return emptyList()

        val sorted = blocks.sortedBy { it.start }
        val merged = mutableListOf<WorklogBlock>()

        var current = sorted[0]

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            val expectedStart = current.start.plus(current.durationSeconds, DateTimeUnit.SECOND)

            if (current.issueKey == next.issueKey && next.start == expectedStart) {
                current = current.copy(durationSeconds = current.durationSeconds + next.durationSeconds)
            } else {
                merged.add(current)
                current = next
            }
        }

        merged.add(current)
        return merged
    }
}
