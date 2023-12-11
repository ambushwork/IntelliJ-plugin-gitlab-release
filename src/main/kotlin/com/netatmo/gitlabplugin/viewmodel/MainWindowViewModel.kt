package com.netatmo.gitlabplugin.viewmodel

import com.netatmo.gitlabplugin.model.GitlabProject
import com.netatmo.gitlabplugin.model.ProjectRelease
import com.netatmo.gitlabplugin.repository.CompositeProjectRepository
import com.netatmo.gitlabplugin.repository.GroupsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainWindowViewModel {

    private val compositeProjectRepository: CompositeProjectRepository = CompositeProjectRepository()

    private val groupsRepository: GroupsRepository = GroupsRepository()

    private val _selectedGroupState = MutableStateFlow<Int?>(null)

    private val _detailState = MutableStateFlow(DetailState())

    internal val detailState = _detailState.asStateFlow()

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

    internal fun selectRelease(projectId: Int, release: ProjectRelease) {
        _detailState.update {
            DetailState(getGitlabProject(projectId), release)
        }
    }

    internal fun selectProject(projectId: Int) {
        _detailState.update {
            DetailState(getGitlabProject(projectId))
        }
    }

    private fun getGitlabProject(projectId: Int): GitlabProject? {
        return compositeProjectRepository.compositeProjectFlow.value.firstOrNull { it.gitlabProject.id == projectId }?.gitlabProject
    }

    internal data class DetailState(
        val gitlabProject: GitlabProject? = null,
        val release: ProjectRelease? = null
    )
}