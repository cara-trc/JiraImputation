package com.jiraimputation.models

import kotlinx.serialization.Serializable

@Serializable
sealed class LogEntry {

    @Serializable
    data class BranchLog(
        val branch: String,
        val timestamp: String
    ) : LogEntry()

    @Serializable
    object PauseMarker : LogEntry()
}