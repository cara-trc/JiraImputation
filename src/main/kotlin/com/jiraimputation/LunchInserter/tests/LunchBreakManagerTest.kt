package com.jiraimputation.LunchInserter.tests

import com.jiraimputation.LunchInserter.LunchBreakManager
import com.jiraimputation.models.LogEntry
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class LunchBreakManagerTest {
    @Test
    fun `adds pause if none exists and removes midday work`() {
        val input = listOf(
            LogEntry.BranchLog("feature/ABC", "2025-06-08T10:25:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T10:35:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T11:00:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T11:35:00Z")
        )

        val result = LunchBreakManager(forceLunch = true).applyIfNeeded(input)

        // ✅ Taille attendue = 2 logs conservés + 2 logs forcés + 1 pause
        TestCase.assertEquals(5, result.size)

        // ✅ Pause présente
        TestCase.assertTrue(result.any { it is LogEntry.PauseMarker && it.timestamp.startsWith("2025-06-08T10:35") })

        // ✅ Logs forcés à 11:30 et 12:30
        TestCase.assertTrue(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T11:30:00Z" })

        // ✅ Ancien log à 10:25 toujours là
        TestCase.assertTrue(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T10:25:00Z" })

        // ✅ Logs supprimés
        TestCase.assertFalse(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T10:35:00Z" })
        TestCase.assertFalse(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T11:00:00Z" })
    }



    @Test
    fun `does not add pause if one already exists between 10_30 and 11_30`() {
        val input = listOf(
            LogEntry.BranchLog("feature/ABC", "2025-06-08T10:25:00Z"),
            LogEntry.PauseMarker("2025-06-08T10:45:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T11:35:00Z")
        )

        val result = LunchBreakManager(forceLunch = true).applyIfNeeded(input)

        assertEquals(input, result)
    }

    @Test
    fun `does nothing if forceLunch is false`() {
        val input = listOf(
            LogEntry.BranchLog("feature/ABC", "2025-06-08T10:25:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T10:35:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T11:00:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T11:35:00Z")
        )

        val result = LunchBreakManager(forceLunch = false).applyIfNeeded(input)

        assertEquals(input, result)
    }

    @Test
    fun `inserts pause at 10_30 and removes logs between 10_30 and 11_30`() {
        val logs = listOf(
            LogEntry.BranchLog("feature/ABC", "2025-06-08T09:00:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T10:00:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T10:25:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T10:30:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T11:00:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T11:35:00Z")
        )

        val result = LunchBreakManager(forceLunch = true).applyIfNeeded(logs)

        assertTrue(result.any { it is LogEntry.PauseMarker })
        assertTrue(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T11:30:00Z" })
        assertFalse(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T10:30:00Z" })
        assertFalse(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T11:00:00Z" })
    }

    @Test
    fun `injects pause marker on all 3 days with logs in lunch range`() {
        val logs = listOf(
            // 🔹 Day 1
            LogEntry.BranchLog("feature/ABC", "2025-06-08T10:45:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T12:00:00Z"),

            // 🔹 Day 2
            LogEntry.BranchLog("feature/DEF", "2025-06-09T11:15:00Z"),
            LogEntry.BranchLog("feature/DEF", "2025-06-09T12:00:00Z"),

            // 🔹 Day 3
            LogEntry.BranchLog("feature/XYZ", "2025-06-10T11:00:00Z"),
            LogEntry.BranchLog("feature/XYZ", "2025-06-10T14:00:00Z")
        )

        val result = LunchBreakManager(forceLunch = true).applyIfNeeded(logs)
        val pauseMarkers = result.filterIsInstance<LogEntry.PauseMarker>()

        // ✅ 3 jours => 3 pause markers
        assertEquals(3, pauseMarkers.size)

        assertTrue(pauseMarkers.any { it.timestamp.startsWith("2025-06-08T10:35") })
        assertTrue(pauseMarkers.any { it.timestamp.startsWith("2025-06-09T10:35") })
        assertTrue(pauseMarkers.any { it.timestamp.startsWith("2025-06-10T10:35") })

        // ✅ 3 logs injectés à 11:30
        val injected = result.filterIsInstance<LogEntry.BranchLog>().map { it.timestamp }
        assertTrue(injected.contains("2025-06-08T11:30:00Z"))
        assertTrue(injected.contains("2025-06-09T11:30:00Z"))
        assertTrue(injected.contains("2025-06-10T11:30:00Z"))

        // ✅ Aucun log entre 10:30 et 11:30 sauf celui forcé
        val rangeViolations = injected.filter { it.substring(11, 16) in "10:30".."11:29" && it != "11:30:00Z" }
        assertTrue(rangeViolations.isEmpty())
    }
}
