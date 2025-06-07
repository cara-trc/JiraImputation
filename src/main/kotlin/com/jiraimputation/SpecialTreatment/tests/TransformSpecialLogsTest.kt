package com.jiraimputation.SpecialTreatment.tests

import com.jiraimputation.SpecialTreatment.SpecialsTasks
import com.jiraimputation.SpecialTreatment.TransformSpecialLogs
import com.jiraimputation.models.WorklogBlock
import junit.framework.TestCase.assertEquals
import kotlinx.datetime.Instant
import org.junit.Test

class TransformSpecialLogsTest {

    private val transformer = TransformSpecialLogs()

    @Test
    fun `replaceSpecialIssueKeys remplace support et versions numeriques en gardant l'instant intact`() {
        // Given
        val blocks = listOf(
            WorklogBlock("support", Instant.Companion.parse("2025-06-07T07:00:00Z"), 900),     // 9h UTC+2
            WorklogBlock("2.21.3", Instant.Companion.parse("2025-06-07T08:00:00Z"), 900),       // 10h
            WorklogBlock("JIR-123", Instant.Companion.parse("2025-06-07T09:00:00Z"), 900)       // 11h
        )

        // When
        val result = transformer.replaceSpecialIssueKeys(blocks)

        // Then
        assertEquals(SpecialsTasks.supportCart, result[0].issueKey)
        assertEquals(SpecialsTasks.runManagement, result[1].issueKey)
        assertEquals("JIR-123", result[2].issueKey)

        // On vérifie que les instants sont conservés
        assertEquals(Instant.Companion.parse("2025-06-07T07:00:00Z"), result[0].start)
        assertEquals(Instant.Companion.parse("2025-06-07T08:00:00Z"), result[1].start)
        assertEquals(Instant.Companion.parse("2025-06-07T09:00:00Z"), result[2].start)
    }

    @Test
    fun `replaceSpecialIssueKeys gere support, versions valides, et ignore les cas non concernes`() {
        // Given
        val blocks = listOf(
            WorklogBlock("support", Instant.parse("2025-06-07T07:00:00Z"), 900),
            WorklogBlock("Support", Instant.parse("2025-06-07T07:15:00Z"), 900),
            WorklogBlock("2.3", Instant.parse("2025-06-07T08:00:00Z"), 900),
            WorklogBlock("1.0.0", Instant.parse("2025-06-07T08:15:00Z"), 900),
            WorklogBlock("3.14.159.265", Instant.parse("2025-06-07T08:30:00Z"), 900),
            WorklogBlock("v1.0", Instant.parse("2025-06-07T08:45:00Z"), 900),      // Ne doit pas matcher
            WorklogBlock("2_3_4", Instant.parse("2025-06-07T09:00:00Z"), 900),     // Ne doit pas matcher
            WorklogBlock("JIR-123", Instant.parse("2025-06-07T09:15:00Z"), 900)    // Doit rester intact
        )

        // When
        val result = transformer.replaceSpecialIssueKeys(blocks)

        // Then
        val expectedKeys = listOf(
            SpecialsTasks.supportCart,     // support
            SpecialsTasks.supportCart,     // Support
            SpecialsTasks.runManagement,   // 2.3
            SpecialsTasks.runManagement,   // 1.0.0
            SpecialsTasks.runManagement,   // 3.14.159.265
            "v1.0",                        // inchangé
            "2_3_4",                       // inchangé
            "JIR-123"                      // inchangé
        )

        result.zip(expectedKeys).forEachIndexed { i, (actual, expected) ->
            assertEquals(expected, actual.issueKey)
        }

        // Vérifie que les timestamps sont bien conservés
        blocks.zip(result).forEachIndexed { i, (original, transformed) ->
            assertEquals(original.start, transformed.start)
        }

        // Vérifie que les durées aussi sont conservées
        blocks.zip(result).forEachIndexed { i, (original, transformed) ->
            assertEquals(original.durationSeconds, transformed.durationSeconds)
        }
    }
}