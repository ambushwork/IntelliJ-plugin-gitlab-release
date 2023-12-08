package com.netatmo.gitlabplugin.viewmodel

import com.netatmo.gitlabplugin.repository.GitlabProjectNetworkRepository
import com.netatmo.gitlabplugin.repository.GitlabProjectRepository
import kotlinx.coroutines.flow.*

class MainWindowViewModel {

    private val repository: GitlabProjectRepository = GitlabProjectNetworkRepository()

    private val _selectedGroupState = MutableStateFlow<String?>(null)

    val projectsStateFlow = repository.getProjects().combine(_selectedGroupState.asStateFlow()) { projects, group ->
        group?.let {
            projects.filter { it.namespace.name == group }
        } ?: projects
    }

    val groupStateFlow = repository.getProjects().map { projects ->
        projects.map { it.namespace }.toSet()
    }


    internal fun requestProjects() = repository.requestProjects()

    internal fun changeGroup(group: String) {
        _selectedGroupState.update {
            group
        }
    }
}