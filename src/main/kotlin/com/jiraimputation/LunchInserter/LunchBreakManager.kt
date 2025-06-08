package com.jiraimputation.LunchInserter

import com.jiraimputation.models.LogEntry
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.TimeZone
//FIXME lots of magic numbers in this file. Can create const
class LunchBreakManager(private val forceLunch: Boolean) {
    fun applyIfNeeded(logs: List<LogEntry>): List<LogEntry> {
        if (!forceLunch) return logs

        // group by date
        val groupedByDay = logs.groupBy {
            when (it) {
                is LogEntry.BranchLog -> it.timestamp.substring(0, 10)
                is LogEntry.PauseMarker -> it.timestamp.substring(0, 10)
            }
        }

        val processedLogs = groupedByDay.flatMap { (date, entriesForDay) ->
            val hasPauseBetween12And14 = entriesForDay.any {
                it is LogEntry.PauseMarker &&
                        it.timestamp.substring(11, 16) in "10:00".."11:59"
            }

            if (hasPauseBetween12And14) {
                entriesForDay
            } else {
                val withoutMiddayLogs = entriesForDay.filterNot {
                    it is LogEntry.BranchLog &&
                            it.timestamp.substring(11, 16) in "10:30".."11:30"
                }

                val pause = LogEntry.PauseMarker(
                    LocalDateTime.parse("${date}T10:30")
                        .toInstant(TimeZone.UTC)
                        .toString()
                )

                (withoutMiddayLogs + pause)
                    .sortedBy { entry: LogEntry ->
                        when (entry) {
                            is LogEntry.BranchLog -> entry.timestamp
                            is LogEntry.PauseMarker -> entry.timestamp
                        }
                    }
            }
        }

        return processedLogs
    }

}