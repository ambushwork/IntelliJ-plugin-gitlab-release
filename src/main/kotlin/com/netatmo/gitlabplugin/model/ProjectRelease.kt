package com.netatmo.gitlabplugin.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectRelease(
    val name: String,
    val description: String,
    val created_at: String,
)