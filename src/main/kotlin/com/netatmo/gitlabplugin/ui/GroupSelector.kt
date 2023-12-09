package com.netatmo.gitlabplugin.ui

import com.netatmo.gitlabplugin.model.Group
import javax.swing.JComboBox

class GroupSelector(
    items: Array<Group>,
    onSelectListener: (Int) -> Unit
) : JComboBox<String>(items.map { it.name }.toTypedArray()) {

    init {
        addActionListener {
            onSelectListener(items[selectedIndex].id)
        }
    }
}