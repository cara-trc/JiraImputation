package com.jiraimputation.aggregator

import com.jiraimputation.models.LogEntry
import com.jiraimputation.models.WorklogBlock
import kotlinx.datetime.Instant

const val CHUNK_SIZE = 3

class WorklogAggregator {
    fun splitSequences(logs: List<LogEntry>): List<List<LogEntry.BranchLog>> {
        return logs
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

    fun aggregateLogsToWorklogBlocks(logs: List<LogEntry>): List<WorklogBlock> {
        return splitSequences(logs)
            .flatMap { sequence ->
                val blocks = aggregateSequence(sequence)
                mergeConsecutiveBlocks(blocks)
            }
    }
}




