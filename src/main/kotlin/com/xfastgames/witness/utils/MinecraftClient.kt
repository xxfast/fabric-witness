package com.xfastgames.witness.utils

import net.minecraft.client.MinecraftClient

fun MinecraftClient.closeScreen() {
    this.currentScreen?.close()
    this.setScreen(null)
}