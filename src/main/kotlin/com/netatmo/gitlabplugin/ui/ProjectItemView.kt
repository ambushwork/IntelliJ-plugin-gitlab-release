package com.netatmo.gitlabplugin.ui

import com.netatmo.gitlabplugin.model.GitlabProject
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JPanel


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
            gitlabProject.name
        )
    )
    itemContent.preferredSize = Dimension(300, 30)
    return itemContent
}
