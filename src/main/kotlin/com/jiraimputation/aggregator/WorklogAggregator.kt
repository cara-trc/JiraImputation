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
        return blocks
            .sortedBy { it.start }
            .fold(mutableListOf()) { acc, block ->
                val last = acc.lastOrNull()

                if (
                    last != null &&
                    last.issueKey == block.issueKey &&
                    block.start == last.start.plus(last.durationSeconds, DateTimeUnit.SECOND)
                ) {
                    acc[acc.lastIndex] = last.copy(
                        durationSeconds = last.durationSeconds + block.durationSeconds
                    )
                } else {
                    acc.add(block)
                }

                acc
            }
    }

}




