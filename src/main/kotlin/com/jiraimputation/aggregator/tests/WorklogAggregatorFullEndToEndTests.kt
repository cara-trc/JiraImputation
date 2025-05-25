package com.jiraimputation.aggregator.tests

import com.jiraimputation.aggregator.WorklogAggregator
import com.jiraimputation.models.LogEntry
import com.jiraimputation.models.LogEntry.BranchLog
import org.junit.Test
import junit.framework.TestCase.assertEquals

class WorklogAggregatorFullEndToEndTests {
    @Test
    fun `aggregateLogsToWorklogBlocks handles full end-to-end flow with mixed branches and pauses`() {
        val logs = listOf(
            BranchLog("JIR-1", "2025-05-18T09:00:00Z"),
            BranchLog("JIR-1", "2025-05-18T09:05:00Z"),
            BranchLog("JIR-2", "2025-05-18T09:10:00Z"), // bloc 1 = [JIR-1, JIR-1, JIR-2] → majority = JIR-1

            LogEntry.PauseMarker,

            BranchLog("JIR-2", "2025-05-18T10:00:00Z"),
            BranchLog("JIR-2", "2025-05-18T10:05:00Z"), // bloc 2 = [JIR-2, JIR-2] → duration = 10min

            LogEntry.PauseMarker,

            BranchLog("JIR-3", "2025-05-18T11:00:00Z"), // bloc 3 = [JIR-3] → 5min
            BranchLog("JIR-3", "2025-05-18T11:05:00Z"),
            BranchLog("JIR-3", "2025-05-18T11:10:00Z"),
            BranchLog("JIR-3", "2025-05-18T11:15:00Z") // bloc 3 = [JIR-3 x4] → [JIR-3, JIR-3, JIR-3, JIR-3] → 2 blocs
        )

        val result = WorklogAggregator().aggregateLogsToWorklogBlocks(logs)

        assertEquals(3, result.size)

        assertEquals("JIR-1", result[0].issueKey)
        assertEquals(900, result[0].durationSeconds)
        assertEquals("2025-05-18T09:00:00Z", result[0].start.toString())

        assertEquals("JIR-2", result[1].issueKey)
        assertEquals(600, result[1].durationSeconds)
        assertEquals("2025-05-18T10:00:00Z", result[1].start.toString())

        assertEquals("JIR-3", result[2].issueKey)
        assertEquals(1200, result[2].durationSeconds)
        assertEquals("2025-05-18T11:00:00Z", result[2].start.toString())

    }

    @Test
    fun `aggregateLogsToWorklogBlocks handles pauses that split before 3 logs`() {
        val logs = listOf(
            BranchLog("JIR-1", "2025-05-18T09:00:00Z"),
            BranchLog("JIR-1", "2025-05-18T09:05:00Z"),

            LogEntry.PauseMarker, // coupe avant 3 logs → bloc incomplet de 10min

            BranchLog("JIR-2", "2025-05-18T10:00:00Z"),

            LogEntry.PauseMarker, // coupe après 1 log → bloc incomplet de 5min

            BranchLog("JIR-3", "2025-05-18T11:00:00Z"),
            BranchLog("JIR-3", "2025-05-18T11:05:00Z"),
            BranchLog("JIR-3", "2025-05-18T11:10:00Z"),

            LogEntry.PauseMarker,

            BranchLog("JIR-4", "2025-05-18T12:00:00Z"),
            BranchLog("JIR-4", "2025-05-18T12:05:00Z") // pas de pause après → bloc incomplet à la fin
        )

        val result = WorklogAggregator().aggregateLogsToWorklogBlocks(logs)

        assertEquals(4, result.size)

        // Bloc 1 → 2 logs avant pause
        assertEquals("JIR-1", result[0].issueKey)
        assertEquals(600, result[0].durationSeconds)
        assertEquals("2025-05-18T09:00:00Z", result[0].start.toString())

        // Bloc 2 → 1 log isolé
        assertEquals("JIR-2", result[1].issueKey)
        assertEquals(300, result[1].durationSeconds)
        assertEquals("2025-05-18T10:00:00Z", result[1].start.toString())

        // Bloc 3 → chunk complet de 3 logs → majority = JIR-3
        assertEquals("JIR-3", result[2].issueKey)
        assertEquals(900, result[2].durationSeconds)
        assertEquals("2025-05-18T11:00:00Z", result[2].start.toString())

        // Bloc 4 → bloc final incomplet sans pause mais ok
        assertEquals("JIR-4", result[3].issueKey)
        assertEquals(600, result[3].durationSeconds)
        assertEquals("2025-05-18T12:00:00Z", result[3].start.toString())
    }

    @Test
    fun `aggregateLogsToWorklogBlocks handles absolute chaos without crying`() {
        val logs = listOf(
            // Bruit au début
            LogEntry.PauseMarker,
            LogEntry.PauseMarker,

            // Séquence 1 : 3 logs mélangés → majority = BUG-1
            BranchLog("BUG-1", "2025-05-18T08:00:00Z"),
            BranchLog("FEAT-2", "2025-05-18T08:05:00Z"),
            BranchLog("BUG-1", "2025-05-18T08:10:00Z"),

            LogEntry.PauseMarker,

            // Séquence 2 : un seul log → 5min
            BranchLog("HOTFIX", "2025-05-18T09:00:00Z"),

            LogEntry.PauseMarker,

            // Séquence 3 : 7 logs → 2 chunks + 1 remaining
            BranchLog("SUP-3", "2025-05-18T10:00:00Z"),
            BranchLog("SUP-3", "2025-05-18T10:05:00Z"),
            BranchLog("SUP-3", "2025-05-18T10:10:00Z"),
            BranchLog("SUP-3", "2025-05-18T10:15:00Z"),
            BranchLog("SUP-3", "2025-05-18T10:20:00Z"),
            BranchLog("SUP-3", "2025-05-18T10:25:00Z"),
            BranchLog("SUP-3", "2025-05-18T10:30:00Z"),

            LogEntry.PauseMarker,

            // Séquence 4 : 2 logs, même clé → fusion attendue
            BranchLog("TASK-9", "2025-05-18T11:00:00Z"),
            BranchLog("TASK-9", "2025-05-18T11:05:00Z"),

            LogEntry.PauseMarker,

            // Séquence 5 : 3 logs, même clé → merge → seul bloc
            BranchLog("FEAT-X", "2025-05-18T12:00:00Z"),
            BranchLog("FEAT-X", "2025-05-18T12:05:00Z"),
            BranchLog("FEAT-X", "2025-05-18T12:10:00Z")
        )

        val result = WorklogAggregator().aggregateLogsToWorklogBlocks(logs)

        assertEquals(5, result.size)

        // Bloc 1
        assertEquals("BUG-1", result[0].issueKey)
        assertEquals(900, result[0].durationSeconds)

        // Bloc 2
        assertEquals("HOTFIX", result[1].issueKey)
        assertEquals(300, result[1].durationSeconds)

        // Bloc 3 et 4 : SUP-3 (7 logs → 3+3+1 → merge de 2 blocs) → fusion attendue
        assertEquals("SUP-3", result[2].issueKey)
        assertEquals(2100, result[2].durationSeconds) // 3×5min + 3×5min + 1×5min

        // Bloc 5 : TASK-9 (2 logs) → 10min
        assertEquals("TASK-9", result[3].issueKey)
        assertEquals(600, result[3].durationSeconds)

        // Bloc 6 : FEAT-X (3 logs → full chunk) → 15min
        assertEquals("FEAT-X", result[4].issueKey)
        assertEquals(900, result[4].durationSeconds)

        // Optionnel : on vérifie les timestamps
        assertEquals("2025-05-18T08:00:00Z", result[0].start.toString())
        assertEquals("2025-05-18T12:00:00Z", result[4].start.toString())
    }

    @Test
    fun `aggregateLogsToWorklogBlocks splits when date changes without pause`() {
        val logs = listOf(
            // Séquence sur le 19 mai
            BranchLog("JIR-100", "2025-05-19T23:45:00Z"),
            BranchLog("JIR-100", "2025-05-19T23:50:00Z"),
            BranchLog("JIR-100", "2025-05-19T23:55:00Z"),

            // Suite immédiate mais jour suivant
            BranchLog("JIR-100", "2025-05-20T00:00:00Z"),
            BranchLog("JIR-100", "2025-05-20T00:05:00Z")
        )

        val result = WorklogAggregator().aggregateLogsToWorklogBlocks(logs)

        // ➤ Split attendu en 2 blocs : un le 19 mai, un le 20 mai
        assertEquals(2, result.size)

        // Bloc 1 = 3 logs sur le 19 → chunk complet → 15min
        assertEquals("JIR-100", result[0].issueKey)
        assertEquals(900, result[0].durationSeconds)
        assertEquals("2025-05-19T23:45:00Z", result[0].start.toString())

        // Bloc 2 = 2 logs restants → bloc incomplet → 10min
        assertEquals("JIR-100", result[1].issueKey)
        assertEquals(600, result[1].durationSeconds)
        assertEquals("2025-05-20T00:00:00Z", result[1].start.toString())
    }

}