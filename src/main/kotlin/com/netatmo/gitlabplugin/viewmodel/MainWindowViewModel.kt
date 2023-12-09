package com.netatmo.gitlabplugin.viewmodel

import com.netatmo.gitlabplugin.repository.CompositeProjectRepository
import com.netatmo.gitlabplugin.repository.GroupsRepository
import kotlinx.coroutines.flow.*

class MainWindowViewModel {

    private val compositeProjectRepository: CompositeProjectRepository = CompositeProjectRepository()

    private val groupsRepository: GroupsRepository = GroupsRepository()

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
        }.onEach {
            if (it.size < 20) {
                _selectedGroupState.value?.let { groupId ->
                    compositeProjectRepository.fetchNextPageByGroup(groupId)
                }
            }
        }

    val groupStateFlow = groupsRepository.groupsFlow

    internal fun requestCompositeProjects() {
        compositeProjectRepository.fetch()
        groupsRepository.fetchGroups()
    }

    internal fun searchProjectInGroup(criteria: String) {
        _selectedGroupState.value?.let {
            compositeProjectRepository.searchProjectInGroup(criteria, it)
        }
    }

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