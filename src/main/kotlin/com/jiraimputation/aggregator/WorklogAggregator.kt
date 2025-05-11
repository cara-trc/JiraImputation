package com.jiraimputation.aggregator

import com.jiraimputation.models.BranchLog
import com.jiraimputation.models.WorklogBlock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import kotlin.collections.filter
class WorklogAggregator {

    fun aggregateLogsToWorklogBlocks(logs: List<BranchLog>): List<WorklogBlock> {
        if (logs.isEmpty()) return emptyList()

        val parsedLogs = logs
            .map { Instant.parse(it.timestamp) to it.branch }
            .sortedBy { it.first }

        // 1. On ne démarre qu’à partir du premier timestamp aligné sur un quart d’heure
        val firstValidStart = parsedLogs.firstOrNull { it.first.epochSeconds % (15 * 60) == 0L }
            ?: return emptyList()

        val filtered = parsedLogs.filter { it.first >= firstValidStart.first }

        val result = mutableListOf<WorklogBlock>()
        val chunkSize = 3
        val roundedDuration = 15 * 60 // 15 minutes

        var i = 0
        while (i < filtered.size) {
            val chunk = filtered.subList(i, minOf(i + chunkSize, filtered.size))

            val start = chunk.first().first
            val branches = chunk.map { it.second }
            val dominantBranch = branches
                .groupingBy { it }.eachCount()
                .maxByOrNull { it.value }?.key ?: continue

            val nextLogTime = filtered.getOrNull(i + chunk.size)?.first
            val lastLogTime = chunk.last().first
            val expectedNextTime = lastLogTime.plus(5 * 60, DateTimeUnit.SECOND)

            val isFullChunk = chunk.size == 3
            val isLastChunk = (i + chunk.size >= filtered.size)
            val nextIsDisconnected = nextLogTime != expectedNextTime

            if (!isFullChunk && (isLastChunk || nextIsDisconnected)) {
                // → Cas spécial : on étend le bloc précédent
                val lastBlock = result.lastOrNull()
                if (lastBlock != null) {
                    result[result.lastIndex] = lastBlock.copy(
                        durationSeconds = lastBlock.durationSeconds + roundedDuration
                    )
                } else {
                    // Pas de bloc précédent ? alors on crée quand même ce bloc partiel
                    result.add(WorklogBlock(dominantBranch, start, roundedDuration))
                }
            } else {
                // Bloc normal (complet ou partiel enchaîné)
                result.add(WorklogBlock(dominantBranch, start, roundedDuration))
            }

            i += chunkSize
        }

        return result
    }
}