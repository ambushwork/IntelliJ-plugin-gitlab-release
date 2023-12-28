package com.netatmo.gitlabplugin.model

sealed class DataState<out T> {

    class Success<T>(val data: T, val isComplete: Boolean = false) : DataState<T>()

    class Error(val exception: Throwable) : DataState<Nothing>()

    object Idle : DataState<Nothing>()

}