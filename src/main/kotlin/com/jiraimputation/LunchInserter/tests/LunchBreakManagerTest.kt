package com.jiraimputation.LunchInserter.tests

import com.jiraimputation.LunchInserter.LunchBreakManager
import com.jiraimputation.models.LogEntry
import junit.framework.TestCase
import org.junit.Test

class LunchBreakManagerTest {

    @Test
    fun `adds pause if none exists and removes midday work`() {
        val input = listOf(
            LogEntry.BranchLog("feature/ABC", "2025-06-08T11:55:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T12:35:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T13:00:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T14:05:00Z")
        )

        val result = LunchBreakManager(forceLunch = true).applyIfNeeded(input)

        TestCase.assertEquals(3, result.size)
        TestCase.assertTrue(result.contains(LogEntry.BranchLog("feature/ABC", "2025-06-08T11:55:00Z")))
        TestCase.assertTrue(result.contains(LogEntry.BranchLog("feature/ABC", "2025-06-08T14:05:00Z")))
        TestCase.assertTrue(result.any { it is LogEntry.PauseMarker && it.timestamp.startsWith("2025-06-08T12:00") })

        TestCase.assertFalse(result.any {
            it is LogEntry.BranchLog && it.timestamp in listOf(
                "2025-06-08T12:35:00Z",
                "2025-06-08T13:00:00Z"
            )
        })
    }

    @Test
    fun `does not add pause if one already exists between 12 and 14`() {
        val input = listOf(
            LogEntry.BranchLog("feature/ABC", "2025-06-08T11:55:00Z"),
            LogEntry.PauseMarker("2025-06-08T12:45:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T14:05:00Z")
        )

        val result = LunchBreakManager(forceLunch = true).applyIfNeeded(input)

        TestCase.assertEquals(input, result)
    }

    @Test
    fun `does nothing if forceLunch is false`() {
        val input = listOf(
            LogEntry.BranchLog("feature/ABC", "2025-06-08T11:55:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T12:35:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T13:00:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T14:05:00Z")
        )

        val result = LunchBreakManager(forceLunch = false).applyIfNeeded(input)

        TestCase.assertEquals(input, result)
    }

    @Test
    fun `inserts pause at noon and removes logs between 12_30 and 13_30`() {
        val logs = listOf(

            LogEntry.BranchLog("feature/ABC", "2025-06-08T09:00:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T09:05:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T10:00:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T11:30:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T12:25:00Z"),

            // logs to suppress
            LogEntry.BranchLog("feature/ABC", "2025-06-08T12:30:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T13:00:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T13:30:00Z"),

            // Après-midi
            LogEntry.BranchLog("feature/ABC", "2025-06-08T13:35:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T14:00:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T15:00:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T17:00:00Z")
        )

        val result = LunchBreakManager(forceLunch = true).applyIfNeeded(logs)

        // ✅ PauseMarker bien ajouté
        val pauseMarker = result.filterIsInstance<LogEntry.PauseMarker>().firstOrNull()
        TestCase.assertNotNull(pauseMarker)
        TestCase.assertTrue(pauseMarker!!.timestamp.startsWith("2025-06-08T12:00"))

        // ✅ Logs supprimés entre 12:30 et 13:30
        TestCase.assertFalse(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T12:30:00Z" })
        TestCase.assertFalse(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T13:00:00Z" })
        TestCase.assertFalse(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T13:30:00Z" })

        // ✅ Logs du matin et après-midi toujours présents
        TestCase.assertTrue(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T09:00:00Z" })
        TestCase.assertTrue(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T12:25:00Z" })
        TestCase.assertTrue(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T13:35:00Z" })
        TestCase.assertTrue(result.any { it is LogEntry.BranchLog && it.timestamp == "2025-06-08T17:00:00Z" })

        // ✅ Nombre total attendu = 12 logs initiaux - 3 supprimés + 1 pause = 10
        TestCase.assertEquals(10, result.size)

        // ✅ Chronologie respectée
        val timestamps = result.map {
            when (it) {
                is LogEntry.BranchLog -> it.timestamp
                is LogEntry.PauseMarker -> it.timestamp
            }
        }
        TestCase.assertEquals(timestamps.sorted(), timestamps)
    }
    @Test
    fun `injects pause marker on all 3 days with logs in lunch range`() {
        val logs = listOf(
            // 🔹 Day 1 — logs in lunch range
            LogEntry.BranchLog("feature/ABC", "2025-06-08T09:00:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T12:45:00Z"),
            LogEntry.BranchLog("feature/ABC", "2025-06-08T14:00:00Z"),

            // 🔹 Day 2 — logs in lunch range
            LogEntry.BranchLog("feature/DEF", "2025-06-09T11:00:00Z"),
            LogEntry.BranchLog("feature/DEF", "2025-06-09T13:15:00Z"),
            LogEntry.BranchLog("feature/DEF", "2025-06-09T15:00:00Z"),

            // 🔹 Day 3 — logs in lunch range
            LogEntry.BranchLog("feature/XYZ", "2025-06-10T10:00:00Z"),
            LogEntry.BranchLog("feature/XYZ", "2025-06-10T12:50:00Z"),
            LogEntry.BranchLog("feature/XYZ", "2025-06-10T16:00:00Z")
        )

        val result = LunchBreakManager(forceLunch = true).applyIfNeeded(logs)

        val pauseMarkers = result.filterIsInstance<LogEntry.PauseMarker>()

        // ✅ 3 PauseMarkers, un pour chaque jour
        TestCase.assertEquals(3, pauseMarkers.size)
        TestCase.assertTrue(pauseMarkers.any { it.timestamp.startsWith("2025-06-08T12:30") })
        TestCase.assertTrue(pauseMarkers.any { it.timestamp.startsWith("2025-06-09T12:30") })
        TestCase.assertTrue(pauseMarkers.any { it.timestamp.startsWith("2025-06-10T12:30") })

        // ✅ Tous les logs hors lunch doivent être conservés
        val timestamps = result.filterIsInstance<LogEntry.BranchLog>().map { it.timestamp }

        TestCase.assertTrue("2025-06-08T09:00:00Z" in timestamps)
        TestCase.assertTrue("2025-06-08T14:00:00Z" in timestamps)

        TestCase.assertTrue("2025-06-09T11:00:00Z" in timestamps)
        TestCase.assertTrue("2025-06-09T15:00:00Z" in timestamps)

        TestCase.assertTrue("2025-06-10T10:00:00Z" in timestamps)
        TestCase.assertTrue("2025-06-10T16:00:00Z" in timestamps)

        // ✅ Tous les logs entre 12:30–13:30 doivent être supprimés
        TestCase.assertFalse(timestamps.any { it.substring(11, 16) in "12:30".."13:30" })
    }

}