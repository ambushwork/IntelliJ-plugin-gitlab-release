package com.netatmo.gitlabplugin.repository

import com.netatmo.gitlabplugin.model.CompositeProject
import com.netatmo.gitlabplugin.model.PageInfo
import com.netatmo.gitlabplugin.retrofit.GitlabApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Response

class CompositeProjectRepository {

    val compositeProjectFlow = MutableStateFlow<List<CompositeProject>>(emptyList())

    private val favoriteRepository = ProjectFavoriteRepository()

    val pageFlow: MutableStateFlow<PageInfo?> = MutableStateFlow(null)

    private val _loadingState = MutableStateFlow(false)

    val loadingState = _loadingState.asStateFlow()

    fun getFavProjects(): Flow<List<CompositeProject>> {
        return favoriteRepository.getFavoriteProjectFlow().map { set ->
            set.map { project ->
                CompositeProject(
                    gitlabProject = project,
                    projectReleases = GitlabApi.getReleasesByProject(project.id).body() ?: emptyList()
                )
            }
        }
    }

    fun fetch() = CoroutineScope(Dispatchers.IO).launch {
        _loadingState.update { true }
        GitlabApi.getProjects().apply {
            if (this.isSuccessful) {
                updatePageFlow()
                this.body()?.let { body ->
                    body.map {
                        CompositeProject(
                            gitlabProject = it,
                            projectReleases = emptyList()
                        )
                    }.apply {
                        // Update first with project list
                        compositeProjectFlow.update { this }
                    }.map { incompleteProject ->
                        GitlabApi.getReleasesByProject(incompleteProject.gitlabProject.id).body()?.let { releases ->
                            incompleteProject.copy(
                                projectReleases = releases
                            )
                        } ?: incompleteProject
                    }.apply {
                        _loadingState.update { false }
                        compositeProjectFlow.update { this }
                    }
                }
            } else {
                println(this.errorBody())
            }
        }
    }

    fun fetchNextPageByGroup(groupId: Int) {
        pageFlow.value?.takeIf { it.current < it.total }?.let {
            fetchByGroup(groupId, it.current + 1)
        }
    }

    fun fetchLastPageByGroup(groupId: Int) {
        pageFlow.value?.takeIf { it.current > 1 }?.let {
            fetchByGroup(groupId, it.current - 1)
        }
    }

    fun fetchByGroup(groupId: Int, page: Int? = null) = CoroutineScope(Dispatchers.IO).launch {
        _loadingState.update { true }
        GitlabApi.getProjectByGroup(groupId, page).apply {
            if (isSuccessful) {
                updatePageFlow()
                this.body()?.let { body ->
                    body.map {
                        CompositeProject(
                            gitlabProject = it,
                            projectReleases = emptyList()
                        )
                    }.apply {
                        compositeProjectFlow.update { this }
                    }.map { incompleteProject ->
                        GitlabApi.getReleasesByProject(incompleteProject.gitlabProject.id).body()?.let { releases ->
                            incompleteProject.copy(
                                projectReleases = releases
                            )
                        } ?: incompleteProject
                    }.apply {
                        _loadingState.update { false }
                        compositeProjectFlow.update { this }
                    }
                }
            }
        }
    }

    fun searchProjectInGroup(criteria: String, groupId: Int) = CoroutineScope(Dispatchers.IO).launch {
        _loadingState.update { true }
        GitlabApi.searchProjectInGroup(criteria, groupId).apply {
            if (isSuccessful) {
                updatePageFlow()
                this.body()?.let { body ->
                    body.map {
                        CompositeProject(
                            gitlabProject = it,
                            projectReleases = emptyList()
                        )
                    }.apply {
                        compositeProjectFlow.update { this }
                    }.map { incompleteProject ->
                        GitlabApi.getReleasesByProject(incompleteProject.gitlabProject.id).body()?.let { releases ->
                            incompleteProject.copy(
                                projectReleases = releases
                            )
                        } ?: incompleteProject
                    }.apply {
                        _loadingState.update { false }
                        compositeProjectFlow.update { this }
                    }
                }
            }
        }
    }


    private fun Response<*>.updatePageFlow() {
        pageFlow.update { PageInfo(getCurrentPage(), getTotalPage()) }
    }

    private fun Response<*>.getCurrentPage(): Int {
        return this.headers()["X-Page"]?.toInt() ?: 0
    }

    private fun Response<*>.getTotalPage(): Int {
        return this.headers()["X-Total-Pages"]?.toInt() ?: 0
    }


    private fun mergeProjects(newProjects: List<CompositeProject>) {
        compositeProjectFlow.update { oldProjects ->
            oldProjects.filter { oldProject ->
                newProjects.map { it.gitlabProject.id }.contains(oldProject.gitlabProject.id).not()
            } + newProjects
        }
    }
}