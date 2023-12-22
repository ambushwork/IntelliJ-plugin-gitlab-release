package com.netatmo.gitlabplugin.model

import kotlinx.serialization.Serializable

/**
 * Data class of Gitlab project.
 *
 * this data class must be var for every property and default value for xml serialization
 */
@Serializable
data class GitlabProject(
    var id: Int = 0,
    var name: String = String(),
    var namespace: ProjectNamespace = ProjectNamespace(),
    var updated_at: String = String(),
    var web_url: String = String(),
    var _links: Links = Links()
) {
    override fun toString(): String = name
}