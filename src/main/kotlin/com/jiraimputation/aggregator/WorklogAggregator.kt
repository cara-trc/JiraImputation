package com.jiraimputation.aggregator

import com.jiraimputation.models.BranchLog
import com.jiraimputation.models.WorklogBlock
import kotlinx.datetime.*
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

const val CHUNK_SIZE = 3

class WorklogAggregator {
    fun aggregateLogsToWorklogBlocks(logs: List<BranchLog>): List<WorklogBlock> {
        val chunkDuration = 15.minutes

        if (logs.isEmpty()) return emptyList()

        val parsedLogs = logs.sortedBy { Instant.parse(it.timestamp) }
        val grouped = buildList {
            parsedLogs.chunked(CHUNK_SIZE).forEachIndexed { i, chunk ->
                val times = chunk.map { Instant.parse(it.timestamp) }
                val start = times.first()

                if (chunk.size == CHUNK_SIZE) {
                    val end = times.last().plus(5.minutes)
                    val expectedEnd = start + chunkDuration

                    if (end <= expectedEnd) {
                        val majority = chunk.map { it.branch }
                            .groupingBy { it }
                            .eachCount()
                            .maxByOrNull { it.value }!!
                            .key

                        add(Triple(majority, start, chunkDuration.inWholeSeconds.toInt()))
                    }
                } else {
                    // Bloc final incomplet
                    val end = times.last().plus(5.minutes)
                    val duration = (end.epochSeconds - start.epochSeconds).toInt()
                    val branch = chunk.first().branch
                    add(Triple(branch, start, duration))
                }
            }
        }

        return mergeConsecutiveBlocks(
            grouped.map { (branch, start, duration) ->
                WorklogBlock(issueKey = branch, start = start, durationSeconds = duration)
            }
        )
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




