package com.jiraimputation

import kotlinx.serialization.Serializable

@Serializable
data class BranchLog(
    val timestamp : String,
    val branch : String
)
