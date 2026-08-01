package com.xfastgames.witness.utils

import net.minecraft.client.gui.DrawContext
import kotlin.math.abs
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

/**
 * Annulus between [innerRadius] and [outerRadius] in screen pixels. Scanlines, same as [circle],
 * so thin tutorial attract rings stay crisp at small sizes.
 */
fun ring(
    context: DrawContext,
    centerX: Int,
    centerY: Int,
    outerRadius: Int,
    innerRadius: Int,
    r: Float,
    g: Float,
    b: Float,
    a: Float,
) {
    if (outerRadius <= 0 || a <= 0f) return
    val outer: Int = outerRadius
    val inner: Int = innerRadius.coerceIn(0, outer - 1)
    val color: Int = argb(r, g, b, a)
    val outerSq: Int = outer * outer
    val innerSq: Int = inner * inner
    for (dy in -outer until outer) {
        val ySq: Int = dy * dy
        if (ySq >= outerSq) continue
        val outerHalf: Int = sqrt((outerSq - ySq).toDouble()).roundToInt()
        if (outerHalf <= 0) continue
        if (ySq >= innerSq || inner <= 0) {
            // Full chord: inside the hole's vertical range, draw the whole outer span.
            context.fill(centerX - outerHalf, centerY + dy, centerX + outerHalf, centerY + dy + 1, color)
            continue
        }
        val innerHalf: Int = sqrt((innerSq - ySq).toDouble()).roundToInt()
        // Left and right arcs of the ring.
        if (outerHalf > innerHalf) {
            context.fill(centerX - outerHalf, centerY + dy, centerX - innerHalf, centerY + dy + 1, color)
            context.fill(centerX + innerHalf, centerY + dy, centerX + outerHalf, centerY + dy + 1, color)
        }
    }
}

/**
 * Draws a filled regular hexagon centred on ([centerX], [centerY]), point up, [diameter] pixels from
 * point to point. Scanlines, like [circle], so it sits correctly in an exact box at small sizes.
 *
 * Point up matches the game's hexagon dots and means a hexagon on a horizontal edge is as wide as
 * the line it sits on rather than wider (rules/witness/04-hexagon-dots.md).
 */
fun hexagon(
    context: DrawContext, centerX: Int, centerY: Int, diameter: Int,
    r: Float, g: Float, b: Float, a: Float,
) = hexagon(context, centerX, centerY, diameter, argb(r, g, b, a))

/** As above, taking a packed ARGB colour, for callers that already have one (a panel's dye). */
fun hexagon(
    context: DrawContext, centerX: Int, centerY: Int, diameter: Int, color: Int
) {
    if (diameter <= 0) return
    val radius: Float = diameter / 2f
    // Half the width across the flats, i.e. the widest the shape ever gets.
    val halfFlats: Float = radius * sqrt(3f) / 2f

    for (row in 0 until diameter) {
        val dy: Float = row + .5f - radius
        val distance: Float = abs(dy)
        // Vertical sides for the middle band, then a linear taper to each point.
        val halfWidth: Float =
            if (distance <= radius / 2) halfFlats
            else halfFlats * (radius - distance) / (radius / 2)
        val half: Int = halfWidth.roundToInt()
        if (half <= 0) continue
        context.fill(centerX - half, centerY - radius.toInt() + row, centerX + half, centerY - radius.toInt() + row + 1, color)
    }
}
