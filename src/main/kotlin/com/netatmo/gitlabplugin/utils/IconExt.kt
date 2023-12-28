package com.netatmo.gitlabplugin.utils

import com.intellij.ui.AnimatedIcon
import java.awt.Image
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.SwingConstants

fun getLoadingLabel(): JLabel {
    return JLabel(AnimatedIcon.Default(), SwingConstants.LEFT)
}

fun Any.applyFavoriteIcon(jLabel: JLabel, fav: Boolean) {
    val image = if (fav) getIcon("/drawable/star.png") else getIcon("/drawable/unstar.png")
    jLabel.icon = ImageIcon(image)
}

fun Any.applyNetworkErrorIcon(jLabel: JLabel) {
    val image = getLargeIcon("/drawable/network_error.png")
    jLabel.icon = ImageIcon(image)
}

fun Any.getLargeIcon(iconPath: String): Image {
    val imageIcon = ImageIcon(this::class.java.getResource(iconPath))
    val image: Image = imageIcon.image // transform it
    return image.getScaledInstance(36, 36, Image.SCALE_SMOOTH) // scale it the smooth way
}

fun Any.getIcon(iconPath: String): Image {
    val imageIcon = ImageIcon(this::class.java.getResource(iconPath))
    val image: Image = imageIcon.image // transform it
    return image.getScaledInstance(14, 14, Image.SCALE_SMOOTH) // scale it the smooth way
}