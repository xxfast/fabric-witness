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