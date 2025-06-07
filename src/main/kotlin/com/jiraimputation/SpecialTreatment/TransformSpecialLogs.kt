package com.jiraimputation.SpecialTreatment

import com.jiraimputation.models.WorklogBlock

class TransformSpecialLogs {

   /* fun findAndReplaceSupport(blocks: List<WorklogBlock>) : List<WorklogBlock>{
        return blocks.map { block ->
            if (block.issueKey == "support") {
                block.copy(issueKey = SpecialsTasks.supportCart)
            } else {
                block
            }
        }
    }

    fun findAndReplaceRunManagement(blocks : List<WorklogBlock>) : List<WorklogBlock>{
        val versionPattern = Regex("""^\d+(\.\d+)+$""") // ex: 2.1, 2.21.3, 1.0.0.5

        return blocks.map { block ->
            if (versionPattern.matches(block.issueKey)) {
                block.copy(issueKey = SpecialsTasks.runManagement)
            } else {
                block
            }
        }
    }


    fun addSupportAndRunManagement(blocks: List<WorklogBlock>) : List<WorklogBlock> {
        val blocksWithSupport = findAndReplaceSupport(blocks)
        return findAndReplaceRunManagement(blocksWithSupport)
    }*/

    fun replaceSpecialIssueKeys(blocks: List<WorklogBlock>): List<WorklogBlock> {
        val versionPattern = Regex("""^\d+(\.\d+)+$""") // Ex: 1.2, 2.21.3

        return blocks.map { block ->
            val newIssueKey = when {
                block.issueKey.equals("support", ignoreCase = true) -> SpecialsTasks.supportCart
                versionPattern.matches(block.issueKey) -> SpecialsTasks.runManagement
                else -> block.issueKey
            }
            block.copy(issueKey = newIssueKey)
        }
    }
}