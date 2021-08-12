package com.xfastgames.witness.utils

import net.minecraft.client.MinecraftClient

fun MinecraftClient.closeScreen() {
    this.currentScreen?.onClose()
    this.setScreen(null)
}