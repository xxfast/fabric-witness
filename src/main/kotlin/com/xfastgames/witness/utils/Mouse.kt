package com.xfastgames.witness.utils

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.MouseHandler
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL

fun MouseHandler.show() =
    InputConstants.grabOrReleaseMouse(Minecraft.getInstance().window, GLFW_CURSOR_NORMAL, this.xpos(), this.ypos())

fun MouseHandler.hide() =
    InputConstants.grabOrReleaseMouse(Minecraft.getInstance().window, GLFW_CURSOR_HIDDEN, this.xpos(), this.ypos())

/**
 * [x]/[y] are **GLFW screen** coordinates (same space as [MouseHandler.xpos]/[ypos]),
 * not framebuffer pixels and not GUI-scaled coords.
 */
fun MouseHandler.setPosition(x: Double = this.xpos(), y: Double = this.ypos(), state: Int = GLFW_CURSOR_NORMAL) {
    val client: Minecraft = Minecraft.getInstance()
    InputConstants.grabOrReleaseMouse(client.window, state, x, y)
}

fun MouseHandler.setPosition(position: MousePosition) {
    setPosition(position.x, position.y)
}

/**
 * Move the OS cursor to a **GUI-scaled** position (Screen / mouseMoved space).
 *
 * 26.2 Window splits sizes: [com.mojang.blaze3d.platform.Window.getWidth] is framebuffer,
 * [com.mojang.blaze3d.platform.Window.getScreenWidth] is what GLFW uses for the cursor.
 * Multiplying by framebuffer/guiScale on retina warps 2× off and breaks solver tip-lock.
 */
fun MouseHandler.setGuiPosition(guiX: Double, guiY: Double, state: Int = GLFW_CURSOR_HIDDEN) {
    val window = Minecraft.getInstance().window
    val screenX = guiX * window.screenWidth / window.guiScaledWidth
    val screenY = guiY * window.screenHeight / window.guiScaledHeight
    setPosition(screenX, screenY, state)
}

class MousePosition(val x: Double, val y: Double)

fun MouseHandler.position(): MousePosition = MousePosition(this.xpos(), this.ypos())
