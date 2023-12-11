package com.netatmo.gitlabplugin.callback

import java.awt.event.MouseEvent
import java.awt.event.MouseListener

interface OnClickListener : MouseListener {

    override fun mousePressed(e: MouseEvent?) {
        // do nothing
    }

    override fun mouseReleased(e: MouseEvent?) {
        // do nothing
    }

    override fun mouseEntered(e: MouseEvent?) {
        // do nothing
    }

    override fun mouseExited(e: MouseEvent?) {
        // do nothing
    }
}