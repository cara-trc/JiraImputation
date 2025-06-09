import com.jiraimputation.CalendarIntegration.MeetingIntegrator
import com.jiraimputation.models.WorklogBlock
import junit.framework.TestCase.assertEquals
import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


class InsertMeetingsInBlocksTest {

    val meetingIntegrator = MeetingIntegrator()
    @Test
    fun `Blocks starts before meeting`() {
        val git = WorklogBlock("GIT", Instant.parse("2025-06-08T09:30:00Z"), 60 * 60)
        val meeting = WorklogBlock("MEETING", Instant.parse("2025-06-08T10:00:00Z"), 60 * 60)

        val result = meetingIntegrator.insertMeetingsInBlocks(listOf(git), listOf(meeting))

        assertEquals(2, result.size)

        assertEquals("GIT", result[0].issueKey)
        assertEquals(Instant.parse("2025-06-08T09:30:00Z"), result[0].start)
        assertEquals(30 * 60, result[0].durationSeconds)

        assertEquals("MEETING", result[1].issueKey)
    }

    @Test
    fun `Block fully in meeting`() {
        val git = WorklogBlock("GIT", Instant.parse("2025-06-08T10:15:00Z"), 30 * 60)
        val meeting = WorklogBlock("MEETING", Instant.parse("2025-06-08T10:00:00Z"), 60 * 60)

        val result = meetingIntegrator.insertMeetingsInBlocks(listOf(git), listOf(meeting))

        assertEquals(1, result.size)
        assertEquals("MEETING", result[0].issueKey)
    }

    @Test
    fun `Blocks starts during the meeting`() {
        val git = WorklogBlock("GIT", Instant.parse("2025-06-08T10:30:00Z"), 60 * 60)
        val meeting = WorklogBlock("MEETING", Instant.parse("2025-06-08T10:00:00Z"), 60 * 60)

        val result = meetingIntegrator.insertMeetingsInBlocks(listOf(git), listOf(meeting))

        assertEquals(2, result.size)

        assertEquals("MEETING", result[0].issueKey)

        assertEquals("GIT", result[1].issueKey)
        assertEquals(Instant.parse("2025-06-08T11:00:00Z"), result[1].start)
        assertEquals(30 * 60, result[1].durationSeconds)
    }

    @Test
    fun `Block start before and end after the meeting`() {
        val git = WorklogBlock("GIT", Instant.parse("2025-06-08T09:30:00Z"), 180 * 60)
        val meeting = WorklogBlock("MEETING", Instant.parse("2025-06-08T10:30:00Z"), 60 * 60)

        val result = meetingIntegrator.insertMeetingsInBlocks(listOf(git), listOf(meeting))
        assertEquals(3, result.size)

        assertEquals("GIT", result[0].issueKey)
        assertEquals(Instant.parse("2025-06-08T09:30:00Z"), result[0].start)
        assertEquals(60 * 60, result[0].durationSeconds)

        assertEquals("MEETING", result[1].issueKey)

        assertEquals("GIT", result[2].issueKey)
        assertEquals(Instant.parse("2025-06-08T11:30:00Z"), result[2].start)
        assertEquals(60 * 60, result[2].durationSeconds)
    }
    @Test
    fun `Journée complète avec plusieurs réunions - insert + découpes`() {
        val base = Instant.parse("2025-06-08T09:00:00Z")

        // 9h → 18h full time work
        val workBlock = WorklogBlock("GIT", base, 9 * 60 * 60)

        val meetings = listOf(
            WorklogBlock("MEETING", Instant.parse("2025-06-08T09:30:00Z"), 30 * 60),
            WorklogBlock("MEETING", Instant.parse("2025-06-08T14:00:00Z"), 60 * 60),
            WorklogBlock("MEETING", Instant.parse("2025-06-08T16:00:00Z"), 90 * 60),
        )

        val result = meetingIntegrator.insertMeetingsInBlocks(listOf(workBlock), meetings)


        assertEquals(7, result.size) // ✅ il y a bien 7 blocs

        val expected = listOf(
            WorklogBlock("GIT", Instant.parse("2025-06-08T09:00:00Z"), 30.minutes.inWholeSeconds.toInt()),
            WorklogBlock("MEETING", Instant.parse("2025-06-08T09:30:00Z"), 30.minutes.inWholeSeconds.toInt()),
            WorklogBlock("GIT", Instant.parse("2025-06-08T10:00:00Z"), 4.hours.inWholeSeconds.toInt()),
            WorklogBlock("MEETING", Instant.parse("2025-06-08T14:00:00Z"), 1.hours.inWholeSeconds.toInt()),
            WorklogBlock("GIT", Instant.parse("2025-06-08T15:00:00Z"), 1.hours.inWholeSeconds.toInt()),
            WorklogBlock("MEETING", Instant.parse("2025-06-08T16:00:00Z"), 90.minutes.inWholeSeconds.toInt()),
            WorklogBlock("GIT", Instant.parse("2025-06-08T17:30:00Z"), 30.minutes.inWholeSeconds.toInt()),
        )
        assertEquals(expected, result)
    }

}
