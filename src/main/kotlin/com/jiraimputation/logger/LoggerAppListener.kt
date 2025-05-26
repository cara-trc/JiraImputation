package com.jiraimputation.logger

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.application.ApplicationManager
// Kinda weird but i need this import event if the compiler say it's not needed
import com.jiraimputation.logger.LoggerFile

class LoggerAppListener : ApplicationInitializedListener {
    override fun componentsInitialized() {
        ApplicationManager.getApplication().getService(LoggerFile::class.java)
    }
}
