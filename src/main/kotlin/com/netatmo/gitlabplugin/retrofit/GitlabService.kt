package com.netatmo.gitlabplugin.retrofit


import com.netatmo.gitlabplugin.model.Project
import retrofit2.http.GET

/**
 * Retrofit service interface for gitlab API
 */
interface GitlabService {

    @GET("projects")
    suspend fun getProjectList(): List<Project>
}