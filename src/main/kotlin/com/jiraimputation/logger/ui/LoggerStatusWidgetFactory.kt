package com.jiraimputation.logger.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class TrackingStatusWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "TrackingStatusWidget"
    override fun getDisplayName(): String = "Tracking Toggle"
    override fun isAvailable(project: Project): Boolean = true
    override fun createWidget(project: Project): StatusBarWidget = LoggerStatusWidget()
    override fun disposeWidget(widget: StatusBarWidget) {}
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}
