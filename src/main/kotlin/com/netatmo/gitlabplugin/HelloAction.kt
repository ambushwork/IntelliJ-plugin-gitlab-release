package com.netatmo.gitlabplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.netatmo.gitlabplugin.retrofit.GitlabApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HelloAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {

        GlobalScope.launch {
            val projects = GitlabApi.getProjects()
            println(projects)
        }

    }
}