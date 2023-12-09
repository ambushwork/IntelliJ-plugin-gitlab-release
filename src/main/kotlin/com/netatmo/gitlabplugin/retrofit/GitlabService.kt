package com.netatmo.gitlabplugin.retrofit


import com.netatmo.gitlabplugin.model.GitlabProject
import com.netatmo.gitlabplugin.model.Group
import com.netatmo.gitlabplugin.model.ProjectRelease
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for gitlab API
 */
interface GitlabService {

    @GET("groups")
    suspend fun getGroups(): Response<List<Group>>

    @GET("projects")
    suspend fun getProjectList(): Response<List<GitlabProject>>


    @GET("groups/{id}/projects")
    suspend fun getProjectByGroup(
        @Path("id") id: Int,
        @Query("page") page: Int? = null
    ): Response<List<GitlabProject>>

    @GET("groups/{id}/projects")
    suspend fun searchProjectInGroup(
        @Path("id") id: Int,
        @Query("search") search: String,
    ): Response<List<GitlabProject>>


    @GET("projects/{id}/releases")
    suspend fun getReleasesByProject(@Path("id") id: Int): Response<List<ProjectRelease>>
}