
import com.jiraimputation.aggregator.WorklogAggregator
import com.jiraimputation.models.BranchLog
import junit.framework.TestCase.assertEquals
import kotlinx.datetime.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
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
    fun `less than 3 logs still creates one 15min block`() {
        val logs = listOf(
            BranchLog("2025-05-11T09:00:00Z", "JIR-1"),
            BranchLog("2025-05-11T09:05:00Z", "JIR-1")
        )

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        assertEquals(1, result.size)
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
    fun `filters logs before first quarter hour`() {
        val logs = listOf(
            BranchLog("2025-05-11T08:57:00Z", "JIR-1"),
            BranchLog("2025-05-11T09:00:00Z", "JIR-1"),
            BranchLog("2025-05-11T09:05:00Z", "JIR-1"),
            BranchLog("2025-05-11T09:10:00Z", "JIR-1")
        )

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        assertEquals(1, result.size)
        assertEquals(Instant.parse("2025-05-11T09:00:00Z"), result.first().start)
    }

}
