package com.jiraimputation.aggregator.tests

import com.jiraimputation.aggregator.WorklogAggregator
import com.jiraimputation.models.WorklogBlock
import org.junit.Test
import junit.framework.TestCase.assertEquals
import kotlinx.datetime.Instant


class MergeConsecutiveBlocksTest {

    private val aggregator = WorklogAggregator()

    @Test
    fun `mergeConsecutiveBlocks merges two identical issueKey blocks`() {
        val blocks = listOf(
            WorklogBlock("JIR-1", Instant.parse("2025-05-18T09:00:00Z"), 900),
            WorklogBlock("JIR-1", Instant.parse("2025-05-18T11:00:00Z"), 600)
        )

        val result = aggregator.mergeConsecutiveBlocks(blocks)

        assertEquals(1, result.size)
        assertEquals("JIR-1", result[0].issueKey)
        assertEquals(1500, result[0].durationSeconds)
        assertEquals(Instant.parse("2025-05-18T09:00:00Z"), result[0].start)
    }

    @Test
    fun `mergeConsecutiveBlocks does not merge non-consecutive same issueKey blocks`() {
        val blocks = listOf(
            WorklogBlock("JIR-1", Instant.parse("2025-05-18T09:00:00Z"), 900),
            WorklogBlock("JIR-2", Instant.parse("2025-05-18T09:30:00Z"), 300),
            WorklogBlock("JIR-1", Instant.parse("2025-05-18T10:00:00Z"), 600)
        )

        val result = aggregator.mergeConsecutiveBlocks(blocks)

        assertEquals(3, result.size)
        assertEquals(listOf("JIR-1", "JIR-2", "JIR-1"), result.map { it.issueKey })
    }

    @Test
    fun `mergeConsecutiveBlocks merges a full chain of identical issueKeys`() {
        val blocks = listOf(
            WorklogBlock("TASK-1", Instant.parse("2025-05-18T08:00:00Z"), 300),
            WorklogBlock("TASK-1", Instant.parse("2025-05-18T09:00:00Z"), 300),
            WorklogBlock("TASK-1", Instant.parse("2025-05-18T10:00:00Z"), 900)
        )

        val result = aggregator.mergeConsecutiveBlocks(blocks)

        assertEquals(1, result.size)
        assertEquals("TASK-1", result[0].issueKey)
        assertEquals(1500, result[0].durationSeconds)
    }

    @Test
    fun `mergeConsecutiveBlocks handles empty list`() {
        val result = aggregator.mergeConsecutiveBlocks(emptyList())
        assertEquals(0, result.size)
    }

    @Test
    fun `mergeConsecutiveBlocks handles single block`() {
        val blocks = listOf(
            WorklogBlock("JIR-1", Instant.parse("2025-05-18T08:00:00Z"), 900)
        )

        val result = aggregator.mergeConsecutiveBlocks(blocks)

        assertEquals(1, result.size)
        assertEquals("JIR-1", result[0].issueKey)
    }

    @Test
    fun `mergeConsecutiveBlocks merges some but not all blocks`() {
        val blocks = listOf(
            WorklogBlock("JIR-1", Instant.parse("2025-05-18T09:00:00Z"), 300),
            WorklogBlock("JIR-1", Instant.parse("2025-05-18T09:30:00Z"), 300),
            WorklogBlock("JIR-2", Instant.parse("2025-05-18T10:00:00Z"), 900)
        )

        val result = aggregator.mergeConsecutiveBlocks(blocks)

        assertEquals(2, result.size)
        assertEquals("JIR-1", result[0].issueKey)
        assertEquals(600, result[0].durationSeconds)
        assertEquals("JIR-2", result[1].issueKey)
    }
}
