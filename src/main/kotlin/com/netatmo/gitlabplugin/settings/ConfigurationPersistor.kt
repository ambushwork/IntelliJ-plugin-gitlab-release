package com.netatmo.gitlabplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Persistor for the plugin preferences.
 */
@State(
    name = "com.netatmo.gitlabplugin.settings.ConfigurationPersistor",
    storages = [Storage("GitlabPluginConfiguration.xml")]
)
class ConfigurationPersistor : PersistentStateComponent<Config> {

    companion object {
        fun getInstance(): ConfigurationPersistor {
            return ApplicationManager.getApplication().getService(ConfigurationPersistor::class.java)
        }
    }

    var config: Config = Config()

    val gitlabBaseUrl: String
        get() = config.baseUrl

    val token: String
        get() = config.token

    fun saveConfig(config: Config) {
        this.config = config
    }

    override fun getState(): Config {
        return config
    }

    override fun loadState(state: Config) {
        XmlSerializerUtil.copyBean(state, config)
    }
}