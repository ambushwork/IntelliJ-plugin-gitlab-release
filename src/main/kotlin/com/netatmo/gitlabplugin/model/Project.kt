package com.netatmo.gitlabplugin.model

import kotlinx.serialization.Serializable

/**
 * Data class of Gitlab project
 */
@Serializable
data class Project(
    val id: Int,
    val name: String,
)