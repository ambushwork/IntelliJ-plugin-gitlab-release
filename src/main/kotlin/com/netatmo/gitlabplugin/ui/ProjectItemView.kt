package com.netatmo.gitlabplugin.ui

import com.netatmo.gitlabplugin.model.GitlabProject
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JPanel


const val DEFAULT_MARGIN_HALF = 8

fun getProjectItemView(gitlabProject: GitlabProject): JPanel {
    val itemContent = JPanel().apply {
        layout = FlowLayout(FlowLayout.LEFT)
    }

    itemContent.add(
        JLabel(
            gitlabProject.id.toString()
        )
    )
    itemContent.add(
        JLabel(
            gitlabProject.name.toString()
        )
    )
    return itemContent
}
