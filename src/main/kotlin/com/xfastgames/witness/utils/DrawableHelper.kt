package com.xfastgames.witness.utils

import net.minecraft.client.gui.DrawContext
import kotlin.math.roundToInt
import kotlin.math.sqrt

// The 1.17-era Tessellator/BufferBuilder immediate-mode GUI drawing was removed (GUI rendering is
// state/pipeline based since 1.20+/1.21.6). These helpers are reimplemented on top of DrawContext.fill.

private fun argb(r: Float, g: Float, b: Float, a: Float): Int {
    val alpha = (a.coerceIn(0f, 1f) * 255).roundToInt()
    val red = (r.coerceIn(0f, 1f) * 255).roundToInt()
    val green = (g.coerceIn(0f, 1f) * 255).roundToInt()
    val blue = (b.coerceIn(0f, 1f) * 255).roundToInt()
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}

fun fill(
    context: DrawContext,
    x1: Int,
    y1: Int,
    x2: Int,
    y2: Int,
    r: Float,
    g: Float,
    b: Float,
    a: Float
) {
    context.fill(x1, y1, x2, y2, argb(r, g, b, a))
}

/**
 * Draws a filled circle (or part of one) using horizontal scanlines.
 * The [arc] parameter supports the two shapes used by this mod: the full disc (0..360)
 * and the right half-disc (0..180, matching the old `x = cx + r * sin(theta)` sweep).
 */
fun circle(
    context: DrawContext, centerX: Int, centerY: Int, radius: Int,
    r: Float, g: Float, b: Float, a: Float,
    arc: IntRange = 0..360,
    @Suppress("UNUSED_PARAMETER") resolution: Double = 15.0
) {
    if (radius <= 0) return
    val color: Int = argb(r, g, b, a)
    val rightHalfOnly: Boolean = arc.last - arc.first < 360 && arc.first == 0 && arc.last <= 180
    for (dy in -radius until radius) {
        val halfWidth: Int = sqrt((radius * radius - dy * dy).toDouble()).roundToInt()
        if (halfWidth <= 0) continue
        val left: Int = if (rightHalfOnly) centerX else centerX - halfWidth
        context.fill(left, centerY + dy, centerX + halfWidth, centerY + dy + 1, color)
    }
}

fun hexagon(
    context: DrawContext, centerX: Int, centerY: Int, size: Int,
    r: Float, g: Float, b: Float, a: Float,
) {
    // Matches the old placeholder implementation, which just drew a filled square.
    context.fill(centerX, centerY, centerX + size, centerY + size, argb(r, g, b, a))
}
