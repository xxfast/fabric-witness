package com.xfastgames.witness.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.MouseHandler
import org.lwjgl.sdl.SDLMouse.SDL_HideCursor
import org.lwjgl.sdl.SDLMouse.SDL_ShowCursor
import org.lwjgl.sdl.SDLMouse.SDL_WarpMouseInWindow

// 26.3 moved the window to SDL3, whose InputConstants only grabs or releases. Hiding a free cursor
// has no vanilla call any more, and vanilla never shows it again, so every hide() needs a show().

fun MouseHandler.show() {
    SDL_ShowCursor()
}

fun MouseHandler.hide() {
    SDL_HideCursor()
}

/**
 * [x]/[y] are **window** coordinates (same space as [MouseHandler.xpos]/[ypos]),
 * not framebuffer pixels and not GUI-scaled coords.
 */
fun MouseHandler.setPosition(x: Double = this.xpos(), y: Double = this.ypos()) {
    SDL_WarpMouseInWindow(Minecraft.getInstance().window.handle(), x.toFloat(), y.toFloat())
}

fun MouseHandler.setPosition(position: MousePosition) {
    setPosition(position.x, position.y)
}

/**
 * Move the OS cursor to a **GUI-scaled** position (Screen / mouseMoved space).
 *
 * Window splits sizes: [com.mojang.blaze3d.platform.Window.getWidth] is framebuffer,
 * [com.mojang.blaze3d.platform.Window.getScreenWidth] is what SDL uses for the cursor.
 * Multiplying by framebuffer/guiScale on retina warps 2× off and breaks solver tip-lock.
 */
fun MouseHandler.setGuiPosition(guiX: Double, guiY: Double) {
    val window = Minecraft.getInstance().window
    val screenX = guiX * window.screenWidth / window.guiScaledWidth
    val screenY = guiY * window.screenHeight / window.guiScaledHeight
    setPosition(screenX, screenY)
}

class MousePosition(val x: Double, val y: Double)

fun MouseHandler.position(): MousePosition = MousePosition(this.xpos(), this.ypos())
