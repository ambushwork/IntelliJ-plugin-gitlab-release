package com.netatmo.gitlabplugin.retrofit


import com.netatmo.gitlabplugin.model.GitlabProject
import com.netatmo.gitlabplugin.model.ProjectRelease
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit service interface for gitlab API
 */
interface GitlabService {

    @GET("projects")
    suspend fun getProjectList(): List<GitlabProject>

    @GET("groups/{id}/projects")
    suspend fun getProjectInGroup(@Path("id") id: Int): List<GitlabProject>

    @GET("projects/{id}/releases")
    suspend fun getReleasesByProject(@Path("id") id: Int): List<ProjectRelease>
}