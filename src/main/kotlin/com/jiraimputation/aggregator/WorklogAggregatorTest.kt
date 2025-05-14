import com.jiraimputation.aggregator.WorklogAggregator
import com.jiraimputation.models.BranchLog
import junit.framework.TestCase.assertEquals
import kotlinx.datetime.Instant
import org.junit.Test

class AggregatorTest {

    private val aggregator = WorklogAggregator()

    @Test
    fun `3 logs with same issueKey produce one block`() {
        val logs = listOf(
            BranchLog("2025-05-11T09:00:00Z", "JIR-1"),
            BranchLog("2025-05-11T09:05:00Z", "JIR-1"),
            BranchLog("2025-05-11T09:10:00Z", "JIR-1")
        )

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        assertEquals(1, result.size)
        assertEquals("JIR-1", result[0].issueKey)
        assertEquals(Instant.parse("2025-05-11T09:00:00Z"), result[0].start)
        assertEquals(900, result[0].durationSeconds)
    }

    @Test
    fun `unordered logs are sorted before grouping`() {
        val logs = listOf(
            BranchLog("2025-05-11T11:10:00Z", "JIR-1"),
            BranchLog("2025-05-11T11:00:00Z", "JIR-1"),
            BranchLog("2025-05-11T11:05:00Z", "JIR-1")
        )

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        assertEquals(1, result.size)
        assertEquals(Instant.parse("2025-05-11T11:00:00Z"), result[0].start)
    }

    @Test
    fun `if 3 different branches pick first one`() {
        val logs = listOf(
            BranchLog("2025-05-11T12:00:00Z", "JIR-A"),
            BranchLog("2025-05-11T12:05:00Z", "JIR-B"),
            BranchLog("2025-05-11T12:10:00Z", "JIR-C")
        )

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        assertEquals(1, result.size)
        assertEquals("JIR-A", result[0].issueKey)
    }

    @Test
    fun `majority branch wins even if not first`() {
        val logs = listOf(
            BranchLog("2025-05-11T13:00:00Z", "JIR-2"),
            BranchLog("2025-05-11T13:05:00Z", "JIR-1"),
            BranchLog("2025-05-11T13:10:00Z", "JIR-1")
        )

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        assertEquals(1, result.size)
        assertEquals("JIR-1", result[0].issueKey)
    }

    @Test
    fun `remaining log is correctly handled`() {
        val logs = listOf(
            BranchLog("2025-05-11T08:57:00Z", "JIR-1"),
            BranchLog("2025-05-11T09:02:00Z", "JIR-1"),
            BranchLog("2025-05-11T09:07:00Z", "JIR-1"),
            BranchLog("2025-05-11T09:12:00Z", "JIR-2") // <- bloc final (1 log seul)
        )

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        assertEquals(2, result.size)
        //rounded to 8h45
        assertEquals(Instant.parse("2025-05-11T08:57:00Z"), result[0].start)
        assertEquals(900, result[0].durationSeconds)
        assertEquals("JIR-2", result[1].issueKey)
        assertEquals(Instant.parse("2025-05-11T09:12:00Z"), result[1].start)
        assertEquals(300, result[1].durationSeconds) // 1 log → 5min
    }

    @Test
    fun `chunk with two logs at the end still produces a partial block`() {
        val logs = listOf(
            BranchLog("2025-05-11T14:00:00Z", "JIR-1"),
            BranchLog("2025-05-11T14:05:00Z", "JIR-1"),
            BranchLog("2025-05-11T14:10:00Z", "JIR-1"),
            BranchLog("2025-05-11T14:15:00Z", "JIR-2"),
            BranchLog("2025-05-11T14:20:00Z", "JIR-2")
        )

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        assertEquals(2, result.size)
        assertEquals("JIR-1", result[0].issueKey)
        assertEquals("JIR-2", result[1].issueKey)
        assertEquals(Instant.parse("2025-05-11T14:15:00Z"), result[1].start)
        assertEquals(600, result[1].durationSeconds) // 2 logs → 10min
    }

    @Test
    fun `gap in the middle of day produces two separate blocks`() {
        val logs = listOf(
            // Bloc du matin
            BranchLog("2025-05-11T09:00:00Z", "JIR-1"),
            BranchLog("2025-05-11T09:05:00Z", "JIR-1"),
            BranchLog("2025-05-11T09:10:00Z", "JIR-1"),

            // Gros trou ici : 3h plus tard
            BranchLog("2025-05-11T12:15:00Z", "JIR-2"),
            BranchLog("2025-05-11T12:20:00Z", "JIR-2"),
            BranchLog("2025-05-11T12:25:00Z", "JIR-2")
        )

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        assertEquals(2, result.size)
        assertEquals("JIR-1", result[0].issueKey)
        assertEquals(Instant.parse("2025-05-11T09:00:00Z"), result[0].start)

        assertEquals("JIR-2", result[1].issueKey)
        assertEquals(Instant.parse("2025-05-11T12:15:00Z"), result[1].start)
    }

    @Test
    fun `alternating branches create distinct blocks`() {
        val logs = listOf(
            "2025-05-14T07:00:00Z" to "JIR-1",
            "2025-05-14T07:05:00Z" to "JIR-1",
            "2025-05-14T07:10:00Z" to "JIR-2",

            "2025-05-14T07:15:00Z" to "JIR-2",
            "2025-05-14T07:20:00Z" to "JIR-1",
            "2025-05-14T07:25:00Z" to "JIR-2",

            "2025-05-14T07:30:00Z" to "JIR-1",
            "2025-05-14T07:35:00Z" to "JIR-1",
            "2025-05-14T07:40:00Z" to "JIR-2"
        ).map { (ts, branch) -> BranchLog(timestamp = ts, branch = branch) }

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        assertEquals(3, result.size)

        assertEquals("JIR-1", result[0].issueKey)
        assertEquals(Instant.parse("2025-05-14T07:00:00Z"), result[0].start)
        assertEquals(900, result[0].durationSeconds)

        assertEquals("JIR-2", result[1].issueKey)
        assertEquals(Instant.parse("2025-05-14T07:15:00Z"), result[1].start)
        assertEquals(900, result[1].durationSeconds)

        assertEquals("JIR-1", result[2].issueKey)
        assertEquals(Instant.parse("2025-05-14T07:30:00Z"), result[2].start)
        assertEquals(900, result[2].durationSeconds)
    }

    @Test
    fun `handles pause lunch with incompletes chunk at the end`() {
        val logs = listOf(
            // Matin - JIR-1
            "2025-05-14T07:10:00Z" to "JIR-1", // 09:10 local
            "2025-05-14T07:15:00Z" to "JIR-1",
            "2025-05-14T07:20:00Z" to "JIR-1", // → chunk complet
            "2025-05-14T07:25:00Z" to "JIR-2",
            "2025-05-14T07:30:00Z" to "JIR-2", // → chunk incomplet

            // PAUSE MIDI (rien entre 07:30 et 11:30 UTC)

            // Après-midi - 13h30 local = 11h30 UTC
            "2025-05-14T11:30:00Z" to "JIR-1",
            "2025-05-14T11:35:00Z" to "JIR-2",
            "2025-05-14T11:40:00Z" to "JIR-2", // → chunk complet

            "2025-05-14T11:45:00Z" to "JIR-1",
            "2025-05-14T11:50:00Z" to "JIR-1"  // → chunk incomplet
        ).map { (ts, branch) -> BranchLog(timestamp = ts, branch = branch) }

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        assertEquals(4, result.size)

        // Bloc 1 - chunk complet JIR-1 matin
        assertEquals("JIR-1", result[0].issueKey)
        assertEquals(Instant.parse("2025-05-14T07:10:00Z"), result[0].start) // arrondi bas
        assertEquals(900, result[0].durationSeconds) // 15min pile

        // Bloc 2 - chunk incomplet JIR-2 matin
        assertEquals("JIR-2", result[1].issueKey)
        assertEquals(Instant.parse("2025-05-14T07:25:00Z"), result[1].start)
        assertEquals(600, result[1].durationSeconds) // 2 logs = 10min

        // Bloc 3 - chunk complet JIR-2 aprem
        assertEquals("JIR-2", result[2].issueKey)
        assertEquals(Instant.parse("2025-05-14T11:30:00Z"), result[2].start)
        assertEquals(900, result[2].durationSeconds)

        // Bloc 4 - chunk incomplet JIR-1 aprem
        assertEquals("JIR-1", result[3].issueKey)
        assertEquals(Instant.parse("2025-05-14T11:45:00Z"), result[3].start)
        assertEquals(600, result[3].durationSeconds)
    }

    @Test
    fun `handles pause lunch with completes chunk at the end`() {
        val logs = listOf(
            // Matin - JIR-1
            "2025-05-14T07:10:00Z" to "JIR-1", // 09:10 local
            "2025-05-14T07:15:00Z" to "JIR-1",
            "2025-05-14T07:20:00Z" to "JIR-1", // → chunk complet
            "2025-05-14T07:25:00Z" to "JIR-2",
            "2025-05-14T07:30:00Z" to "JIR-2",
            "2025-05-14T07:35:00Z" to "JIR-2",

            // PAUSE MIDI (rien entre 07:30 et 11:30 UTC)

            // Après-midi - 13h30 local = 11h30 UTC
            "2025-05-14T11:30:00Z" to "JIR-1",
            "2025-05-14T11:35:00Z" to "JIR-2",
            "2025-05-14T11:40:00Z" to "JIR-2", // → chunk complet

        ).map { (ts, branch) -> BranchLog(timestamp = ts, branch = branch) }

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        assertEquals(3, result.size)

        // Bloc 1 - chunk complet JIR-1 matin
        assertEquals("JIR-1", result[0].issueKey)
        assertEquals(Instant.parse("2025-05-14T07:10:00Z"), result[0].start) // arrondi bas
        assertEquals(900, result[0].durationSeconds) // 15min pile

        // Bloc 2 - chunk complet JIR-2 matin
        assertEquals("JIR-2", result[1].issueKey)
        assertEquals(Instant.parse("2025-05-14T07:25:00Z"), result[1].start)
        assertEquals(900, result[1].durationSeconds) // 2 logs = 10min

        // Bloc 3 - chunk complet JIR-2 aprem
        assertEquals("JIR-2", result[2].issueKey)
        assertEquals(Instant.parse("2025-05-14T11:30:00Z"), result[2].start)
        assertEquals(900, result[2].durationSeconds)

    }

}
