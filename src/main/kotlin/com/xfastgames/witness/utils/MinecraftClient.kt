package com.xfastgames.witness.utils

import net.minecraft.client.Minecraft

fun Minecraft.closeScreen() {
    this.gui.screen()?.onClose()
    this.gui.setScreen(null)
}

/** Hud only exposes toggle/isHidden; flip until it matches [hidden]. */
fun Minecraft.setHudHidden(hidden: Boolean) {
    val hud = gui.hud
    if (hud.isHidden != hidden) hud.toggle()
}
