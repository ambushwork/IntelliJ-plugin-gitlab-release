package com.netatmo.gitlabplugin.repository

import com.netatmo.gitlabplugin.model.CompositeProject
import com.netatmo.gitlabplugin.retrofit.GitlabApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CompositeProjectRepository {

    val compositeProjectFlow = MutableStateFlow<List<CompositeProject>>(emptyList())

    fun fetch() = CoroutineScope(Dispatchers.IO).launch {
        GitlabApi.getProjects().map {
            CompositeProject(
                gitlabProject = it,
                projectReleases = emptyList()
            )
        }.apply {
            // Update first with project list
            compositeProjectFlow.update { this }
        }.map {
            it.copy(
                projectReleases = GitlabApi.getReleasesByProject(it.gitlabProject.id)
            )
        }.apply {
            compositeProjectFlow.update { this }
        }
    }
}