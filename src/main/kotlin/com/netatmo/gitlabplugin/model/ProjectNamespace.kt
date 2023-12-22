package com.netatmo.gitlabplugin.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectNamespace(
    var id: Int = 0,
    var name: String = String(),
    var path: String = String(),
)