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
    fun `splitSequences survives chaotic real-life log layout with mixed branches and scattered pauses`() {
        val logs = listOf(
            // bruit initial
            LogEntry.PauseMarker,
            LogEntry.PauseMarker,

            // bloc 1 : 2 logs, même branche
            BranchLog("FEAT-1", "2025-05-18T09:00:00Z"),
            BranchLog("FEAT-1", "2025-05-18T09:05:00Z"),

            // pause isolée
            LogEntry.PauseMarker,

            // bloc 2 : logs en désordre
            BranchLog("BUG-42", "2025-05-18T09:10:00Z"),
            BranchLog("BUG-42", "2025-05-18T09:15:00Z"),
            BranchLog("FEAT-2", "2025-05-18T09:20:00Z"),
            BranchLog("BUG-42", "2025-05-18T09:25:00Z"),
            BranchLog("FEAT-2", "2025-05-18T09:30:00Z"),

            // pause
            LogEntry.PauseMarker,

            // bloc 3 : 1 seul log
            BranchLog("HOTFIX", "2025-05-18T09:35:00Z"),

            // pause inutile
            LogEntry.PauseMarker,
            LogEntry.PauseMarker,

            // bloc 4 : 8 logs mélangés
            BranchLog("FEAT-3", "2025-05-18T09:40:00Z"),
            BranchLog("FEAT-3", "2025-05-18T09:45:00Z"),
            BranchLog("FEAT-3", "2025-05-18T09:50:00Z"),
            BranchLog("BUG-13", "2025-05-18T09:55:00Z"),
            BranchLog("FEAT-3", "2025-05-18T10:00:00Z"),
            BranchLog("BUG-13", "2025-05-18T10:05:00Z"),
            BranchLog("FEAT-3", "2025-05-18T10:10:00Z"),
            BranchLog("FEAT-3", "2025-05-18T10:15:00Z"),

            // pause de fin
            LogEntry.PauseMarker
        )

        val result = aggregator.splitSequences(logs)

        // 4 séquences valides attendues
        assertEquals(4, result.size)

        // Bloc 1 = [FEAT-1, FEAT-1]
        assertEquals(listOf("FEAT-1", "FEAT-1"), result[0].map { it.branch })

        // Bloc 2 = [BUG-42, BUG-42, FEAT-2, BUG-42, FEAT-2]
        assertEquals(5, result[1].size)
        assertEquals(listOf("BUG-42", "BUG-42", "FEAT-2", "BUG-42", "FEAT-2"), result[1].map { it.branch })

        // Bloc 3 = [HOTFIX]
        assertEquals(1, result[2].size)
        assertEquals("HOTFIX", result[2][0].branch)

        // Bloc 4 = 8 logs
        assertEquals(8, result[3].size)
        assertEquals(
            listOf("FEAT-3", "FEAT-3", "FEAT-3", "BUG-13", "FEAT-3", "BUG-13", "FEAT-3", "FEAT-3"),
            result[3].map { it.branch }
        )
    }

}
