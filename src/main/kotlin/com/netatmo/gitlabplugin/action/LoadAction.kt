package com.netatmo.gitlabplugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.util.PlatformIcons.SYNCHRONIZE_ICON

class LoadAction(val onLoad: () -> Unit) : AnAction(SYNCHRONIZE_ICON) {
    override fun actionPerformed(e: AnActionEvent) {
        onLoad()
    }
}