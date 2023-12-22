package com.netatmo.gitlabplugin.utils

import com.netatmo.gitlabplugin.ui.GitlabProjectsWindow
import java.awt.Image
import javax.swing.ImageIcon
import javax.swing.JLabel


fun GitlabProjectsWindow.applyFavoriteIcon(jLabel: JLabel, fav: Boolean) {
    val image = if (fav) getIcon("/drawable/star.png") else getIcon("/drawable/unstar.png")
    jLabel.icon = ImageIcon(image)
}

fun Any.getIcon(iconPath: String): Image {
    val imageIcon = ImageIcon(this::class.java.getResource(iconPath))
    val image: Image = imageIcon.image // transform it
    return image.getScaledInstance(20, 20, Image.SCALE_SMOOTH) // scale it the smooth way
}