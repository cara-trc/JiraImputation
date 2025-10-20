package com.jiraimputation.SpecialTreatment

import com.jiraimputation.JiraSettings
import com.jiraimputation.models.WorklogBlock

class TransformSpecialLogs {

    fun replaceSpecialIssueKeys(blocks: List<WorklogBlock>): List<WorklogBlock> {
        val versionPattern = Regex("""^\d+(\.\d+)+$""") // Ex: 1.2, 2.21.3

        return blocks.map { block ->
            val newIssueKey = when {
                block.issueKey.equals("support", ignoreCase = true) -> JiraSettings.supportCard
                versionPattern.matches(block.issueKey) -> JiraSettings.runManagement
                else -> block.issueKey
            }
            block.copy(issueKey = newIssueKey)
        }
    }
}