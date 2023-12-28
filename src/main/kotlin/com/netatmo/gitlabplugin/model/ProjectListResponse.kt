package com.netatmo.gitlabplugin.model

data class ProjectListResponse(
    val projects: List<CompositeProject>,
    val pageInfo: PageInfo? = null
)