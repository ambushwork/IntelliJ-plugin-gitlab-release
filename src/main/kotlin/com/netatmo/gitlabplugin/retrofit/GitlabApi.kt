package com.netatmo.gitlabplugin.retrofit

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

/**
 * Retrofit API declaration for gitlab-plugin network API
 */
object GitlabApi {

    private val baseUrl = "https://gitlab.corp.netatmo.com/api/v4/"
    private val token = "glpat-rR_446FUxFaVNsfMCpEE"

    private val retrofit: GitlabService

    init {
        val contentType = "application/json".toMediaType()
        val okHttpClient = okHttpClient(token)
        val serializeJson = Json { ignoreUnknownKeys = true }
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .validateEagerly(true)
            .client(okHttpClient)
            .addConverterFactory(serializeJson.asConverterFactory(contentType))
            .build()
            .create(GitlabService::class.java)

    }

    /**
     * API to get gitlab projects
     */
    suspend fun getProjects() = retrofit.getProjectList()

}