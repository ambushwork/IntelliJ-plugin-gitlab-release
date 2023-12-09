package com.netatmo.gitlabplugin.viewmodel

import com.netatmo.gitlabplugin.repository.CompositeProjectRepository
import kotlinx.coroutines.flow.*

class MainWindowViewModel {

    private val compositeProjectRepository: CompositeProjectRepository = CompositeProjectRepository()

    private val _selectedGroupState = MutableStateFlow<Int?>(null)

    private val _selectedRelease = MutableStateFlow<Pair<String, Int>?>(null)

    val selectedRelease =
        compositeProjectRepository.compositeProjectFlow.combine(_selectedRelease) { compositeProjects, pair ->
            pair?.let {
                compositeProjects
                    .firstOrNull { it.gitlabProject.id == pair.second }?.projectReleases?.firstOrNull { it.name == pair.first }
            }
        }

    val compositeProjectFlow = compositeProjectRepository.compositeProjectFlow
        .combine(_selectedGroupState.asStateFlow()) { projects, group ->
            group?.let {
                projects.filter { it.gitlabProject.namespace.id == group }
            } ?: projects
        }

    val groupStateFlow = compositeProjectFlow.map { projects ->
        projects.map { it.gitlabProject.namespace }.toSet()
    }

    internal fun requestCompositeProjects() = compositeProjectRepository.fetch()

    internal fun changeGroup(groupId: Int) {
        _selectedGroupState.update {
            groupId
        }
    }

    internal fun selectRelease(projectId: Int, name: String) {
        _selectedRelease.update {
            Pair(name, projectId)
        }
    }
}