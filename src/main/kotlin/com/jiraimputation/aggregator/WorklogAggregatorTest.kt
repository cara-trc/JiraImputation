
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
    fun `6 logs produce 2 blocks`() {
        val logs = (0 until 6).map {
            val time = Instant.parse("2025-05-11T10:00:00Z").plus(it * 5 * 60, DateTimeUnit.SECOND)
            BranchLog(time.toString(), "JIR-1")
        }

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        assertEquals(2, result.size)
        assertEquals(Instant.parse("2025-05-11T10:00:00Z"), result[0].start)
        assertEquals(Instant.parse("2025-05-11T10:15:00Z"), result[1].start)
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

    @Test
    fun `full day with lunch break creates clean blocks`() {
        val logs = mutableListOf<BranchLog>()

        // Matin : 09h00 → 12h00
        var current = Instant.parse("2025-05-11T09:00:00Z")
        while (current < Instant.parse("2025-05-11T12:00:00Z")) {
            logs.add(BranchLog(current.toString(), "JIR-1"))
            current = current.plus(5 * 60, DateTimeUnit.SECOND)
        }

        // Aprem : 14h00 → 18h30
        current = Instant.parse("2025-05-11T14:00:00Z")
        while (current < Instant.parse("2025-05-11T18:30:00Z")) {
            logs.add(BranchLog(current.toString(), "JIR-1"))
            current = current.plus(5 * 60, DateTimeUnit.SECOND)
        }

        val result = aggregator.aggregateLogsToWorklogBlocks(logs)

        // 90 logs → 30 blocs (15 minutes chacun)
        assertEquals(30, result.size)
        assertEquals(Instant.parse("2025-05-11T09:00:00Z"), result.first().start)
        assertEquals(Instant.parse("2025-05-11T18:15:00Z"), result.last().start)
        result.forEach { assertEquals(900, it.durationSeconds) }
    }
}
