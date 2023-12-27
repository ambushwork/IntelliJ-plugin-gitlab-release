package com.netatmo.gitlabplugin.storage

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.netatmo.gitlabplugin.model.GitlabProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Service
@State(
    name = "FavProjectState",
    //->/workspace/gitlab-plugin/build/idea-sandbox/config/options/fav.xml
    storages = [Storage("fav2.xml")]
)
object FavProjectStateService : PersistentStateComponent<FavProjectStateService.State> {

    private var favFlow = MutableStateFlow(State())

    class State(var favProjects: Set<GitlabProject> = emptySet())

    override fun getState(): State {
        return favFlow.value
    }

    override fun loadState(state: State) {
        val newState = State()
        XmlSerializerUtil.copyBean(state, newState)
        favFlow.update {
            newState
        }
    }

    fun getFavoriteProjectFlow(): Flow<Set<GitlabProject>> {
        return favFlow.map { it.favProjects }
    }

    fun addFavoriteProject(project: GitlabProject) {
        this.favFlow.update {
            State(this.state.favProjects + project)
        }
    }

    fun removeFavoriteProject(projectId: Int) {
        this.favFlow.update {
            State(this.state.favProjects.filter { it.id != projectId }.toSet())
        }
    }

    fun isFavoriteProject(projectId: Int): Boolean {
        return this.favFlow.value.favProjects.any { it.id == projectId }
    }
}

