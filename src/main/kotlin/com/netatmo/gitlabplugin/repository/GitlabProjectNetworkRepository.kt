package com.netatmo.gitlabplugin.repository

import com.netatmo.gitlabplugin.model.GitlabProject
import com.netatmo.gitlabplugin.retrofit.GitlabApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GitlabProjectNetworkRepository : GitlabProjectRepository {

    private val _projects = MutableStateFlow(emptyList<GitlabProject>())


    override fun getProjects(): Flow<List<GitlabProject>> {
        return _projects.asStateFlow()
    }

    override fun requestProjects() {
        CoroutineScope(Dispatchers.Default).launch {
            _projects.update {
                GitlabApi.getProjects()
            }
        }
    }
}