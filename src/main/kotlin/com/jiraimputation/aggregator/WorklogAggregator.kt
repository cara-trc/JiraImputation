package com.jiraimputation.aggregator

import com.jiraimputation.models.BranchLog
import com.jiraimputation.models.WorklogBlock
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.minutes

class WorklogAggregator {

    fun aggregateLogsToWorklogBlocks(logs: List<BranchLog>): List<WorklogBlock> {
        if (logs.isEmpty()) return emptyList()

        val parsedLogs = logs.map {
            it.branch to Instant.parse(it.timestamp)
        }.sortedBy { it.second }

        val chunkSize = 3 // 3 logs de 5 min = 15 min
        val chunkDuration = 15.minutes

        val grouped = mutableListOf<Triple<String, Instant, Int>>()
        var index = 0

        while (index + chunkSize <= parsedLogs.size) {
            val chunk = parsedLogs.subList(index, index + chunkSize)
            val (branches, times) = chunk.map { it.first } to chunk.map { it.second }

            val start = times.first().roundToQuarterHour()
            val end = times.last().plus(5.minutes)
            val expectedEnd = start.plus(chunkDuration)

            // Si le chunk n'est pas propre (trou), skip 1
            if (end > expectedEnd) {
                index += 1
                continue
            }

            val majorityBranch = branches.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key
            grouped.add(Triple(majorityBranch, start, chunkDuration.inWholeSeconds.toInt()))
            index += chunkSize
        }

        // ✅ Nouveau : gestion du dernier bloc incomplet à la fin
        if (index < parsedLogs.size) {
            val remaining = parsedLogs.subList(index, parsedLogs.size)
            val (branches, times) = remaining.map { it.first } to remaining.map { it.second }

            val start = times.first().roundToQuarterHour()
            val majorityBranch = branches.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key

            grouped.add(Triple(majorityBranch, start, chunkDuration.inWholeSeconds.toInt()))
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
