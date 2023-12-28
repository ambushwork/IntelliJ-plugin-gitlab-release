package com.netatmo.gitlabplugin.repository

import com.netatmo.gitlabplugin.model.*
import com.netatmo.gitlabplugin.retrofit.GitlabApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response

class CompositeProjectRepository {

    val dataStateFlow = MutableStateFlow<DataState<ProjectListResponse>>(DataState.Idle)

    private val favoriteRepository = ProjectFavoriteRepository()

    fun getFavProjects(): Flow<DataState<ProjectListResponse>> {
        return favoriteRepository.getFavoriteProjectFlow().map { set ->
            val newSet = set.map { project ->
                CompositeProject(
                    gitlabProject = project,
                    projectReleases = GitlabApi.getReleasesByProject(project.id).body() ?: emptyList()
                )
            }
            DataState.Success(
                ProjectListResponse(
                    projects = newSet.toList(),
                    pageInfo = null,
                ),
                isComplete = true
            )
        }
    }


    fun fetch() = CoroutineScope(Dispatchers.IO).launch {
        GitlabApi.getProjects().parseResponse()
    }

    private suspend fun Response<List<GitlabProject>>.parseResponse() {
        if (this.isSuccessful) {
            val pageInfo = PageInfo(getCurrentPage(), getTotalPage())
            this.body()?.let { body ->
                body.map {
                    CompositeProject(
                        gitlabProject = it,
                        projectReleases = emptyList()
                    )
                }.apply {
                    // Update first with project list
                    dataStateFlow.update {
                        DataState.Success(
                            data = ProjectListResponse(
                                projects = this,
                                pageInfo = pageInfo
                            ),
                            isComplete = false
                        )
                    }
                }.map { incompleteProject ->
                    GitlabApi.getReleasesByProject(incompleteProject.gitlabProject.id).body()?.let { releases ->
                        incompleteProject.copy(
                            projectReleases = releases
                        )
                    } ?: incompleteProject
                }.apply {
                    dataStateFlow.update {
                        DataState.Success(
                            data = ProjectListResponse(
                                projects = this,
                                pageInfo = pageInfo
                            ),
                            isComplete = true
                        )
                    }
                }
            }
        } else {
            dataStateFlow.update {
                DataState.Error(
                    Exception("network error.")
                )
            }
        }
    }

    fun fetchByGroup(groupId: Int, page: Int? = null) = CoroutineScope(Dispatchers.IO).launch {
        GitlabApi.getProjectByGroup(groupId, page).parseResponse()
    }

    fun searchProjectInGroup(criteria: String, groupId: Int) = CoroutineScope(Dispatchers.IO).launch {
        GitlabApi.searchProjectInGroup(criteria, groupId).parseResponse()
    }

    private fun Response<*>.getCurrentPage(): Int {
        return this.headers()["X-Page"]?.toInt() ?: 0
    }

    private fun Response<*>.getTotalPage(): Int {
        return this.headers()["X-Total-Pages"]?.toInt() ?: 0
    }
}