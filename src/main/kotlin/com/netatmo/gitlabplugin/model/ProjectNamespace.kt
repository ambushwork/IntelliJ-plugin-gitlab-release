package com.netatmo.gitlabplugin.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectNamespace(
    val id: Int,
    val name: String,
    val path: String,
)