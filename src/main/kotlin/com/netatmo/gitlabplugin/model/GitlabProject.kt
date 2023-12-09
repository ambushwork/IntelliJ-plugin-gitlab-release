package com.netatmo.gitlabplugin.model

import kotlinx.serialization.Serializable

/**
 * Data class of Gitlab project
 */
@Serializable
data class GitlabProject(
    val id: Int,
    val name: String,
    val namespace: ProjectNamespace
) {
    override fun toString(): String = name
}