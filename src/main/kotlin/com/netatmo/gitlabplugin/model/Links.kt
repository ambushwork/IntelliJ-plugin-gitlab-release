package com.netatmo.gitlabplugin.model

import kotlinx.serialization.Serializable

@Serializable
data class Links(
    var self: String = String(),
    //val merge_requests: String,
    //val repo_branches: String,
)