package com.netatmo.gitlabplugin.repository

import com.netatmo.gitlabplugin.model.CompositeProject
import com.netatmo.gitlabplugin.model.PageInfo
import com.netatmo.gitlabplugin.retrofit.GitlabApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response

class CompositeProjectRepository {

    val compositeProjectFlow = MutableStateFlow<List<CompositeProject>>(emptyList())

    val pageFlow: MutableStateFlow<PageInfo?> = MutableStateFlow(null)

    fun fetch() = CoroutineScope(Dispatchers.IO).launch {
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
                        compositeProjectFlow.update { this }
                    }
                }
            }
        }
    }

    fun searchProjectInGroup(criteria: String, groupId: Int) = CoroutineScope(Dispatchers.IO).launch {
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
                        compositeProjectFlow.update { this }
                    }
                }
            }
        }
    }

    private fun getLastProjectIdInGroup(groupId: Int): Int {
        return compositeProjectFlow.value.last { it.gitlabProject.namespace.id == groupId }.gitlabProject.id
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