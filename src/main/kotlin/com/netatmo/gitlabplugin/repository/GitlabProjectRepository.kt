package com.netatmo.gitlabplugin.repository

import com.netatmo.gitlabplugin.model.GitlabProject
import kotlinx.coroutines.flow.Flow

interface GitlabProjectRepository {

    fun getProjects(): Flow<List<GitlabProject>>

    fun requestProjects()
}