package com.jiraimputation.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.ide.util.PropertiesComponent
import com.jiraimputation.LunchInserter.LunchUserPreference

class LunchBreakToggleAction : ToggleAction("Force Lunch Break") {

    override fun isSelected(e: AnActionEvent): Boolean {
        return LunchUserPreference.forceLaunch
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        LunchUserPreference.forceLaunch = state
        PropertiesComponent.getInstance().setValue("LunchBreakForce", state)
    }
}