package com.netatmo.gitlabplugin.repository

import com.netatmo.gitlabplugin.model.Group
import com.netatmo.gitlabplugin.retrofit.GitlabApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupsRepository {

    val groupsFlow = MutableStateFlow<List<Group>>(emptyList())

    fun fetchGroups() {
        CoroutineScope(Dispatchers.Default).launch {
            GitlabApi.getGroups().apply {
                if (isSuccessful) {
                    this.body()?.let { groups ->
                        groupsFlow.update {
                            groups
                        }
                    }
                } else {
                    println("error")
                }
            }
        }
    }
}