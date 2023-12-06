package com.netatmo.gitlabplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.JOptionPane

class HelloAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        JOptionPane.showMessageDialog(null, "Hello, Android Studio Plugin!")
    }
}