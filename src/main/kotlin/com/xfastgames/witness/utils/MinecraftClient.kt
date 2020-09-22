package com.xfastgames.witness.utils

import net.minecraft.client.MinecraftClient

fun MinecraftClient.closeScreen(): Unit {
    this.currentScreen?.onClose()
    this.openScreen(null)
}