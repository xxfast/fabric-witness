package com.xfastgames.witness.utils

import net.minecraft.client.MinecraftClient
import net.minecraft.client.Mouse
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL

fun Mouse.show() =
    InputUtil.setCursorParameters(MinecraftClient.getInstance().window.handle, GLFW_CURSOR_NORMAL, this.x, this.y)

fun Mouse.hide() =
    InputUtil.setCursorParameters(MinecraftClient.getInstance().window.handle, GLFW_CURSOR_HIDDEN, this.x, this.y)

fun Mouse.setPosition(x: Double = this.x, y: Double = this.y, state: Int = GLFW_CURSOR_NORMAL) {
    val client: MinecraftClient = MinecraftClient.getInstance()
    InputUtil.setCursorParameters(client.window.handle, state, x, y)
}

fun Mouse.setPosition(position: MousePosition) {
    setPosition(position.x, position.y)
}

class MousePosition(val x: Double, val y: Double)

fun Mouse.position(): MousePosition = MousePosition(this.x, this.y)