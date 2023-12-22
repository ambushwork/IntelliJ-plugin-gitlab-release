package com.netatmo.gitlabplugin.repository

import com.intellij.openapi.components.ServiceManager
import com.netatmo.gitlabplugin.model.GitlabProject
import com.netatmo.gitlabplugin.storage.FavProjectStateService

class ProjectFavoriteRepository {

    private val favoriteProjectStateService = ServiceManager.getService(FavProjectStateService::class.java)

    fun getFavoriteProjectFlow() = favoriteProjectStateService.getFavoriteProjectFlow()

    fun setProjectFav(project: GitlabProject, fav: Boolean) {
        if (fav) {
            favoriteProjectStateService.addFavoriteProject(project)
        } else {
            favoriteProjectStateService.removeFavoriteProject(project.id)
        }
    }

    fun isFav(projectId: Int): Boolean {
        return favoriteProjectStateService.isFavoriteProject(projectId)
    }


}