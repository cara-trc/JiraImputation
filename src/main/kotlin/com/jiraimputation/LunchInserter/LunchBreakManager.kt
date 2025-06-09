package com.jiraimputation.LunchInserter

import com.jiraimputation.models.LogEntry
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.minutes

class LunchBreakManager(private val forceLunch: Boolean) {


    fun applyIfNeeded(logs: List<LogEntry>): List<LogEntry> {
        if (!forceLunch) return logs

        val groupedByDay = logs.groupBy {
            when (it) {
                is LogEntry.BranchLog -> it.timestamp.substring(0, 10)
                is LogEntry.PauseMarker -> it.timestamp.substring(0, 10)
            }
        }

        return groupedByDay.flatMap { (date, dailyLogs) ->
            val (year, month, day) = date.split("-").map { it.toInt() }
            val lunchStart = LocalDateTime(year, month, day, 10, 30)
            val lunchEnd = LocalDateTime(year, month, day, 11, 30)

            val hasPauseAlready = dailyLogs.any {
                it is LogEntry.PauseMarker &&
                        it.timestamp.substring(11, 16) in "10:30".."11:30"
            }

            if (hasPauseAlready) return@flatMap dailyLogs

            val logsToKeep = dailyLogs.filterNot {
                it is LogEntry.BranchLog &&
                        it.timestamp.substring(11, 16) >= "10:30" &&
                        it.timestamp.substring(11, 16) < "11:30"
            }

            val logsBefore = logsToKeep.filterIsInstance<LogEntry.BranchLog>()
                .filter { it.timestamp.substring(11, 16) < "10:30" }

            val logsAfter = logsToKeep.filterIsInstance<LogEntry.BranchLog>()
                .filter { it.timestamp.substring(11, 16) >= "11:30" }

            val (forcedBefore, forcedAfter) = forceBoundsIfNeeded(logsBefore, logsAfter, lunchStart, lunchEnd)

            val pauseInstant = lunchStart.toInstant(TimeZone.UTC) + 5.minutes
            val pauseMarker = LogEntry.PauseMarker(pauseInstant.toString())

            (forcedBefore + pauseMarker + forcedAfter)
                .sortedBy {
                    when (it) {
                        is LogEntry.BranchLog -> it.timestamp
                        is LogEntry.PauseMarker -> it.timestamp
                    }
                }
        }
    }




    private fun forceBoundsIfNeeded(
        before: List<LogEntry.BranchLog>,
        after: List<LogEntry.BranchLog>,
        lunchStart: LocalDateTime,
        lunchEnd: LocalDateTime
    ): Pair<List<LogEntry>, List<LogEntry>> {
        val resultBefore = before.toMutableList()
        val resultAfter = after.toMutableList()

        val lastBefore = before.lastOrNull()
        val firstAfter = after.firstOrNull()

        if (lastBefore != null) {
            val fiveMinBeforeLunch = lunchStart.toInstant(TimeZone.UTC) - 5.minutes
            resultBefore.add(
                LogEntry.BranchLog(
                    timestamp = fiveMinBeforeLunch.toString(), branch = lastBefore.branch
                )
            )
        }

        if (firstAfter != null) {
            resultAfter.add(
                LogEntry.BranchLog(
                    timestamp = lunchEnd.toInstant(TimeZone.UTC).toString(), branch = firstAfter.branch
                )
            )
        }

        return resultBefore to resultAfter
    }
}
