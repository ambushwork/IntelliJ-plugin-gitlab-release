package com.netatmo.gitlabplugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class ConfigurationScreen : Configurable {

    private val settingsPanel: JPanel
    private val gitlabBaseUrlField = JBTextField(ConfigurationPersistor.getInstance().gitlabBaseUrl)
    private val tokenField = JBTextField(ConfigurationPersistor.getInstance().token)

    init {
        settingsPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Gitlab url: "), gitlabBaseUrlField, 1, false)
            .addLabeledComponent(JBLabel("Access token: "), tokenField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun createComponent(): JComponent {
        return settingsPanel
    }

    override fun isModified(): Boolean {
        val settings = ConfigurationPersistor.getInstance()
        return gitlabBaseUrlField.text != settings.gitlabBaseUrl || tokenField.text != settings.token
    }

    override fun apply() {
        ConfigurationPersistor.getInstance().apply {
            saveConfig(Config(gitlabBaseUrlField.text, tokenField.text))
        }
    }

    override fun getDisplayName(): String {
        return "Gitlab plugin settings"
    }

}