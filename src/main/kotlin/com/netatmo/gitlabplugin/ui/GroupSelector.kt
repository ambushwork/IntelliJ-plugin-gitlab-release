package com.netatmo.gitlabplugin.ui

import javax.swing.JComboBox

class GroupSelector(
    items: Array<out String>,
    onSelectListener: (String) -> Unit
) : JComboBox<String>(items) {

    init {
        addActionListener {
            onSelectListener(items[selectedIndex])
        }
    }
}