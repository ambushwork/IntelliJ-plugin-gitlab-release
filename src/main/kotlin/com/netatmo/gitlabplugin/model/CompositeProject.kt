package com.netatmo.gitlabplugin.model

/**
 * Data class of a composite entity merged from /projects API and /releases API
 */
data class CompositeProject(
    val gitlabProject: GitlabProject,
    val projectReleases: List<ProjectRelease>
)