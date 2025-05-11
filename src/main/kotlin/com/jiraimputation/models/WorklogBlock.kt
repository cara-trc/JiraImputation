package com.jiraimputation.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class WorklogBlock(
    val issueKey: String,
    val start: Instant,
    val durationSeconds: Int = 900
)
