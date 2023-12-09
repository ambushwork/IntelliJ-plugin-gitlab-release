package com.netatmo.gitlabplugin.ui

import com.netatmo.gitlabplugin.model.ProjectNamespace
import javax.swing.JComboBox

class GroupSelector(
    items: Array<ProjectNamespace>,
    onSelectListener: (Int) -> Unit
) : JComboBox<String>(items.map { it.name }.toTypedArray()) {

    init {
        addActionListener {
            onSelectListener(items[selectedIndex].id)
        }
    }
}