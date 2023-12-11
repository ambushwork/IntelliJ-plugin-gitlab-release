package com.netatmo.gitlabplugin.model

import kotlinx.serialization.Serializable

@Serializable
data class Links(
    val self: String,
    //val merge_requests: String,
    //val repo_branches: String,
)