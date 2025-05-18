package com.jiraimputation.aggregator.tests

import com.jiraimputation.aggregator.WorklogAggregator
import com.jiraimputation.models.LogEntry.BranchLog
import org.junit.Test
import junit.framework.TestCase.assertEquals


class WorklogAggregatorAggregateSequence {
    private val aggregator = WorklogAggregator()

    @Test
    fun `aggregateSequence handles full and partial chunks correctly`() {
        val sequence = listOf(
            BranchLog("A", "2025-05-18T09:00:00Z"),
            BranchLog("A", "2025-05-18T09:05:00Z"),
            BranchLog("A", "2025-05-18T09:10:00Z"), // bloc 1 : full (3) → majoritaire = A

            BranchLog("B", "2025-05-18T09:15:00Z"),
            BranchLog("C", "2025-05-18T09:20:00Z")  // bloc 2 : partial (2) → 1er log = B
        )

        val result = aggregator.aggregateSequence(sequence)

        assertEquals(2, result.size)

        val firstBlock = result[0]
        assertEquals("A", firstBlock.issueKey)
        assertEquals(900, firstBlock.durationSeconds)
        assertEquals("2025-05-18T09:00:00Z", firstBlock.start.toString())

        val secondBlock = result[1]
        assertEquals("B", secondBlock.issueKey)
        assertEquals(600, secondBlock.durationSeconds)
        assertEquals("2025-05-18T09:15:00Z", secondBlock.start.toString())
    }

    @Test
    fun `aggregateSequence resolves majority key correctly in full chunk`() {
        val sequence = listOf(
            BranchLog("A", "2025-05-18T09:00:00Z"),
            BranchLog("A", "2025-05-18T09:05:00Z"),
            BranchLog("B", "2025-05-18T09:10:00Z") // bloc 1 → majorité A
        )

        val result = aggregator.aggregateSequence(sequence)

        assertEquals(1, result.size)
        assertEquals("A", result[0].issueKey)
        assertEquals(900, result[0].durationSeconds)
    }

    @Test
    fun `aggregateSequence resolves complete chunk tie by majority and not by first key`() {
        val sequence = listOf(
            BranchLog("A", "2025-05-18T09:00:00Z"),
            BranchLog("B", "2025-05-18T09:05:00Z"),
            BranchLog("B", "2025-05-18T09:10:00Z") // égalité A(1) B(2) → majorité B
        )

        val result = aggregator.aggregateSequence(sequence)

        assertEquals("B", result[0].issueKey)
    }

    @Test
    fun `aggregateSequence takes first log key for incomplete chunk`() {
        val sequence = listOf(
            BranchLog("A", "2025-05-18T09:00:00Z"),
            BranchLog("B", "2025-05-18T09:05:00Z")
        )

        val result = aggregator.aggregateSequence(sequence)

        assertEquals(1, result.size)
        assertEquals("A", result[0].issueKey)
        assertEquals(600, result[0].durationSeconds)
    }

    @Test
    fun `aggregateSequence handles multiple chunks with mixed logic`() {
        val sequence = listOf(
            BranchLog("A", "2025-05-18T09:00:00Z"),
            BranchLog("B", "2025-05-18T09:05:00Z"),
            BranchLog("B", "2025-05-18T09:10:00Z"), // bloc 1 → majorité B

            BranchLog("C", "2025-05-18T09:15:00Z"),
            BranchLog("D", "2025-05-18T09:20:00Z")  // bloc 2 → clé = C
        )

        val result = aggregator.aggregateSequence(sequence)

        assertEquals(2, result.size)

        assertEquals("B", result[0].issueKey)
        assertEquals(900, result[0].durationSeconds)

        assertEquals("C", result[1].issueKey)
        assertEquals(600, result[1].durationSeconds)
    }

    @Test
    fun `aggregateSequence handles single log chunk correctly`() {
        val sequence = listOf(
            BranchLog("Z", "2025-05-18T09:00:00Z")
        )

        val result = aggregator.aggregateSequence(sequence)

        assertEquals(1, result.size)
        assertEquals("Z", result[0].issueKey)
        assertEquals(300, result[0].durationSeconds)
    }
    @Test
    fun `aggregateSequence handles 7 logs with mixed keys and validates both majority and remaining`() {
        val sequence = listOf(
            BranchLog("A", "2025-05-18T09:00:00Z"),
            BranchLog("B", "2025-05-18T09:05:00Z"),
            BranchLog("B", "2025-05-18T09:10:00Z"), // Bloc 1 → majorité = B

            BranchLog("C", "2025-05-18T09:15:00Z"),
            BranchLog("C", "2025-05-18T09:20:00Z"),
            BranchLog("C", "2025-05-18T09:25:00Z"), // Bloc 2 → majorité = C

            BranchLog("D", "2025-05-18T09:30:00Z")  // Bloc 3 (remaining) → clé = D
        )

        val result = aggregator.aggregateSequence(sequence)

        assertEquals(3, result.size)

        // Bloc 1
        assertEquals("B", result[0].issueKey)
        assertEquals("2025-05-18T09:00:00Z", result[0].start.toString())
        assertEquals(900, result[0].durationSeconds)

        // Bloc 2
        assertEquals("C", result[1].issueKey)
        assertEquals("2025-05-18T09:15:00Z", result[1].start.toString())
        assertEquals(900, result[1].durationSeconds)

        // Bloc 3
        assertEquals("D", result[2].issueKey)
        assertEquals("2025-05-18T09:30:00Z", result[2].start.toString())
        assertEquals(300, result[2].durationSeconds)
    }







}