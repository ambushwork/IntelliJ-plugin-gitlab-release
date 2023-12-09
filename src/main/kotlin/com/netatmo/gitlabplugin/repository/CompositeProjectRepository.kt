package com.netatmo.gitlabplugin.repository

import com.netatmo.gitlabplugin.model.CompositeProject
import com.netatmo.gitlabplugin.retrofit.GitlabApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response

class CompositeProjectRepository {

    val compositeProjectFlow = MutableStateFlow<List<CompositeProject>>(emptyList())

    var currentPage: Int = 0

    fun fetch() = CoroutineScope(Dispatchers.IO).launch {
        GitlabApi.getProjects().apply {
            if (this.isSuccessful) {
                currentPage = getCurrentPage()
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
        fetchByGroup(groupId, currentPage + 1)
    }

    fun fetchByGroup(groupId: Int, page: Int? = null) = CoroutineScope(Dispatchers.IO).launch {
        GitlabApi.getProjectByGroup(groupId, page).apply {
            if (isSuccessful) {
                currentPage = getCurrentPage()
                this.body()?.let { body ->
                    body.map {
                        CompositeProject(
                            gitlabProject = it,
                            projectReleases = emptyList()
                        )
                    }.apply {
                        mergeProjects(this)
                    }.map { incompleteProject ->
                        GitlabApi.getReleasesByProject(incompleteProject.gitlabProject.id).body()?.let { releases ->
                            incompleteProject.copy(
                                projectReleases = releases
                            )
                        } ?: incompleteProject
                    }.apply {
                        mergeProjects(this)
                    }
                }
            }
        }
    }

    fun searchProjectInGroup(criteria: String, groupId: Int) = CoroutineScope(Dispatchers.IO).launch {
        GitlabApi.searchProjectInGroup(criteria, groupId).apply {
            if (isSuccessful) {
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

    private fun Response<*>.getCurrentPage(): Int {
        return this.headers()["X-Page"]?.toInt() ?: 0
    }


    private fun mergeProjects(newProjects: List<CompositeProject>) {
        compositeProjectFlow.update { oldProjects ->
            oldProjects.filter { oldProject ->
                newProjects.map { it.gitlabProject.id }.contains(oldProject.gitlabProject.id).not()
            } + newProjects
        }
    }
}