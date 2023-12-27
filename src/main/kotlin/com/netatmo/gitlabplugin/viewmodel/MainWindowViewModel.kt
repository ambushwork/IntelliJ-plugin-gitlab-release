package com.netatmo.gitlabplugin.viewmodel

import com.netatmo.gitlabplugin.model.GitlabProject
import com.netatmo.gitlabplugin.model.PageInfo
import com.netatmo.gitlabplugin.model.ProjectRelease
import com.netatmo.gitlabplugin.repository.CompositeProjectRepository
import com.netatmo.gitlabplugin.repository.GroupsRepository
import com.netatmo.gitlabplugin.repository.ProjectFavoriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class MainWindowViewModel {

    private val compositeProjectRepository: CompositeProjectRepository = CompositeProjectRepository()

    private val projectFavoriteRepository = ProjectFavoriteRepository()

    private val groupsRepository: GroupsRepository = GroupsRepository()

    private val _selectedGroupState = MutableStateFlow<Int?>(null)

    private val _detailState = MutableStateFlow(DetailState())

    private val projectFavFlow = projectFavoriteRepository.getFavoriteProjectFlow()

    private val _favState = MutableStateFlow(false)

    internal val toolbarState =
        combine(
            _favState,
            compositeProjectRepository.loadingState,
            compositeProjectRepository.pageFlow
        ) { fav, loading, pageInfo ->
            ToolbarState(loading = loading, favorite = fav, pageInfo = pageInfo)
        }

    val compositeProjectFlow = combine(
        _favState,
        compositeProjectRepository.compositeProjectFlow,
        compositeProjectRepository.getFavProjects()
    ) { fav, projects, favs ->
        if (fav) {
            favs
        } else {
            projects
        }
    }

    internal val detailState = _detailState.combine(projectFavFlow) { detailState, favs ->
        detailState.copy(
            favorite = favs.any { it.id == detailState.gitlabProject?.id }
        )
    }

    val groupStateFlow = groupsRepository.groupsFlow

    internal fun toggleFavorite() {
        _favState.update { it.not() }
    }

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
            DetailState(
                getGitlabProject(projectId),
                release,
                favorite = isFavorite(projectId)
            )
        }
    }

    internal fun selectProject(projectId: Int) {
        _detailState.update {
            DetailState(
                getGitlabProject(projectId),
                favorite = isFavorite(projectId)
            )
        }
    }

    internal fun toggleProjectFav(projectId: Int) {
        getGitlabProject(projectId)?.let {
            projectFavoriteRepository.setProjectFav(it, isFavorite(projectId).not())
        }
    }

    private fun getGitlabProject(projectId: Int): GitlabProject? {
        return compositeProjectRepository.compositeProjectFlow.value.firstOrNull { it.gitlabProject.id == projectId }?.gitlabProject
    }

    private fun isFavorite(projectId: Int): Boolean {
        return projectFavoriteRepository.isFav(projectId)
    }

    internal data class DetailState(
        val gitlabProject: GitlabProject? = null,
        val release: ProjectRelease? = null,
        val favorite: Boolean = false
    )

    internal data class ToolbarState(
        val loading: Boolean = false,
        val favorite: Boolean = false,
        val pageInfo: PageInfo? = null,
    )
}