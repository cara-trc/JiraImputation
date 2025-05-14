package com.jiraimputation.logger.ui

import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget.TextPresentation
import com.intellij.util.Consumer
import com.jiraimputation.logger.LoggerState
import java.awt.Component
import java.awt.event.MouseEvent

class LoggerStatusWidget : StatusBarWidget, TextPresentation {

    private var statusBar: StatusBar? = null

    override fun ID(): String = "TrackingStatusWidget"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {}

    override fun getText(): String {
        return if (LoggerState.trackingPaused)  "⬤── Imputation OFF" else "──⬤ Imputation ON"
    }

    override fun getTooltipText(): String = "Click to toggle tracking"

    override fun getClickConsumer(): Consumer<MouseEvent>? {
        return Consumer {
            LoggerState.trackingPaused = !LoggerState.trackingPaused
            statusBar?.updateWidget(ID()) // Rafraîchit le texte du widget
        }
    }

    override fun getAlignment(): Float = Component.CENTER_ALIGNMENT
}
