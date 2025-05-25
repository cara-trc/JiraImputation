package com.jiraimputation.aggregator.tests


import com.jiraimputation.aggregator.WorklogAggregator
import com.jiraimputation.models.LogEntry
import com.jiraimputation.models.LogEntry.BranchLog
import org.junit.Test
import junit.framework.TestCase.assertEquals


class WorklogAggregatorSplitTest {

    private val aggregator = WorklogAggregator()

    @Test
    fun `splitSequences returns single sequence when no PauseMarker`() {
        val logs = listOf(
            BranchLog("JIR-1", "2025-05-18T10:00:00Z"),
            BranchLog("JIR-1", "2025-05-18T10:05:00Z")
        )

        val result = aggregator.splitSequences(logs)

        assertEquals(1, result.size)
        assertEquals(2, result[0].size)
    }

    @Test
    fun `splitSequences splits on PauseMarker`() {
        val logs = listOf(
            BranchLog("JIR-1", "2025-05-18T10:00:00Z"),
            LogEntry.PauseMarker,
            BranchLog("JIR-2", "2025-05-18T11:00:00Z")
        )

        val result = aggregator.splitSequences(logs)

        assertEquals(2, result.size)
        assertEquals("JIR-1", result[0][0].branch)
        assertEquals("JIR-2", result[1][0].branch)
    }

    @Test
    fun `splitSequences skips empty blocks between pauses`() {
        val logs = listOf(
            LogEntry.PauseMarker,
            LogEntry.PauseMarker,
            BranchLog("JIR-3", "2025-05-18T12:00:00Z"),
            LogEntry.PauseMarker
        )

        val result = aggregator.splitSequences(logs)

        assertEquals(1, result.size)
        assertEquals("JIR-3", result[0][0].branch)
    }

    @Test
    fun `splitSequences handles trailing entries`() {
        val logs = listOf(
            BranchLog("JIR-1", "2025-05-18T09:00:00Z"),
            LogEntry.PauseMarker,
            BranchLog("JIR-2", "2025-05-18T09:15:00Z"),
            BranchLog("JIR-2", "2025-05-18T09:20:00Z")
        )

        val result = aggregator.splitSequences(logs)

        assertEquals(2, result.size)
        assertEquals(1, result[0].size)
        assertEquals(2, result[1].size)
    }

    @Test
    fun `splitSequences survives chaotic real-life log layout with mixed branches, scattered pauses, and date changes`() {
        val logs = listOf(
            // bruit initial
            LogEntry.PauseMarker,
            LogEntry.PauseMarker,

            // bloc 1 : 2 logs, même branche (jour 1)
            BranchLog("FEAT-1", "2025-05-18T09:00:00Z"),
            BranchLog("FEAT-1", "2025-05-18T09:05:00Z"),

            // pause isolée
            LogEntry.PauseMarker,

            // bloc 2 : logs en désordre (jour 1)
            BranchLog("BUG-42", "2025-05-18T09:10:00Z"),
            BranchLog("BUG-42", "2025-05-18T09:15:00Z"),
            BranchLog("FEAT-2", "2025-05-18T09:20:00Z"),
            BranchLog("BUG-42", "2025-05-18T09:25:00Z"),
            BranchLog("FEAT-2", "2025-05-18T09:30:00Z"),

            // pause
            LogEntry.PauseMarker,

            // bloc 3 : 1 seul log (jour 1)
            BranchLog("HOTFIX", "2025-05-18T09:35:00Z"),

            // pause inutile
            LogEntry.PauseMarker,
            LogEntry.PauseMarker,

            // bloc 4 : 8 logs mélangés (jour 2)
            BranchLog("FEAT-3", "2025-05-19T09:40:00Z"),
            BranchLog("FEAT-3", "2025-05-19T09:45:00Z"),
            BranchLog("FEAT-3", "2025-05-19T09:50:00Z"),
            BranchLog("BUG-13", "2025-05-19T09:55:00Z"),
            BranchLog("FEAT-3", "2025-05-19T10:00:00Z"),
            BranchLog("BUG-13", "2025-05-19T10:05:00Z"),
            BranchLog("FEAT-3", "2025-05-19T10:10:00Z"),
            BranchLog("FEAT-3", "2025-05-19T10:15:00Z"),

            LogEntry.PauseMarker,

            // ❗ bloc 5 : test changement de jour SANS pause
            BranchLog("FEAT-4", "2025-05-19T23:55:00Z"),
            BranchLog("FEAT-4", "2025-05-20T09:00:00Z"),

            // pause de fin
            LogEntry.PauseMarker
        )

        val result = aggregator.splitSequences(logs)

        // 6 séquences attendues :
        // - FEAT-1
        // - BUG-42 / FEAT-2
        // - HOTFIX
        // - FEAT-3 / BUG-13 (jour 2)
        // - FEAT-4 (23:55)
        // - FEAT-4 (09:00 le lendemain)
        assertEquals(6, result.size)

        // Bloc 5 = [FEAT-4] le 19
        assertEquals(listOf("FEAT-4"), result[4].map { it.branch })
        assertEquals("2025-05-19T23:55:00Z", result[4][0].timestamp)

        // Bloc 6 = [FEAT-4] le 20
        assertEquals(listOf("FEAT-4"), result[5].map { it.branch })
        assertEquals("2025-05-20T09:00:00Z", result[5][0].timestamp)
    }


    @Test
    fun splitSequences_shouldSplitWhenDateChanges() {
        val logs = listOf(
            BranchLog(
                branch = "JIR-456",
                timestamp = "2025-05-24T23:55:00Z"
            ),
            BranchLog(
                branch = "JIR-456",
                timestamp = "2025-05-25T09:00:00Z"
            )
        )

        val sequences = WorklogAggregator().splitSequences(logs)
        println(sequences.joinToString("\n") { it.joinToString(", ") { log -> log.timestamp } })

        assertEquals("Il doit y avoir 2 séquences car les logs sont sur 2 jours différents",2, sequences.size)
        assertEquals(1, sequences[0].size)
        assertEquals(1, sequences[1].size)
        assertEquals("2025-05-24T23:55:00Z", sequences[0][0].timestamp)
        assertEquals("2025-05-25T09:00:00Z", sequences[1][0].timestamp)
    }

}
