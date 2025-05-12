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
}
