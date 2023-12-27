package com.netatmo.gitlabplugin.retrofit

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.netatmo.gitlabplugin.model.GitlabProject
import com.netatmo.gitlabplugin.model.Group
import com.netatmo.gitlabplugin.model.ProjectRelease
import com.netatmo.gitlabplugin.settings.ConfigurationPersistor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Response
import retrofit2.Retrofit

/**
 * Retrofit API declaration for gitlab-plugin network API
 */
object GitlabApi {

    private val retrofit: GitlabService

    init {
        val contentType = "application/json".toMediaType()
        val okHttpClient = okHttpClient(ConfigurationPersistor.getInstance().token)
        val serializeJson = Json { ignoreUnknownKeys = true }
        retrofit = Retrofit.Builder()
            .baseUrl(ConfigurationPersistor.getInstance().gitlabBaseUrl)
            .validateEagerly(true)
            .client(okHttpClient)
            .addConverterFactory(serializeJson.asConverterFactory(contentType))
            .build()
            .create(GitlabService::class.java)

    }

    suspend fun getGroups(): Response<List<Group>> = withContext(Dispatchers.IO) {
        retrofit.getGroups()
    }

    /**
     * API to get gitlab projects
     */
    suspend fun getProjects(): Response<List<GitlabProject>> = withContext(Dispatchers.IO) {
        retrofit.getProjectList()
    }

    suspend fun getReleasesByProject(projectId: Int): Response<List<ProjectRelease>> = withContext(Dispatchers.IO) {
        retrofit.getReleasesByProject(projectId)
    }

    suspend fun getProjectByGroup(groupId: Int, page: Int? = null) = withContext(Dispatchers.IO) {
        retrofit.getProjectByGroup(groupId, page)
    }

    suspend fun searchProjectInGroup(criteria: String, groupId: Int) = withContext(Dispatchers.IO) {
        retrofit.searchProjectInGroup(groupId, criteria)
    }
}