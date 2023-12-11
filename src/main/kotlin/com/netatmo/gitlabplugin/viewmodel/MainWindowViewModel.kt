package com.netatmo.gitlabplugin.viewmodel

import com.netatmo.gitlabplugin.repository.CompositeProjectRepository
import com.netatmo.gitlabplugin.repository.GroupsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

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

    val groupStateFlow = groupsRepository.groupsFlow

    val pageFlow = compositeProjectRepository.pageFlow.asStateFlow()

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
        compositeProjectRepository.fetchByGroup(groupId)
    }

    internal fun fetchNextPage() {
        _selectedGroupState.value?.let {
            compositeProjectRepository.fetchNextPageByGroup(it)
        }
    }

    internal fun fetchLastPage() {
        _selectedGroupState.value?.let {
            compositeProjectRepository.fetchLastPageByGroup(it)
        }
    }

    internal fun selectRelease(projectId: Int, name: String) {
        _selectedRelease.update {
            Pair(name, projectId)
        }
    }
}