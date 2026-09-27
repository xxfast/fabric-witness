package com.xfastgames.witness.utils

import com.mojang.blaze3d.vertex.VertexConsumer
import org.joml.Matrix4fc
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max

/**
 * Draws quads through [curve]: each one is cut into cells no longer than [STEP] of the face along
 * either side, and every corner is bent before it reaches [delegate]. A straight lattice edge is one
 * long quad, so without the cut it would stay straight however its four corners moved.
 *
 * Vertices arrive the usual way, a position then its attributes, and a quad is only complete once
 * the next one starts, so [flush] must be called after the last.
 *
 * Runs for every vertex of every screen frame in view, every frame: it allocates nothing per
 * vertex. The first version did (a closure per attribute, a pair per bend), and a row of three
 * screens lost 11 to 15 fps with 4 fps hitches against the curve switched off (2026-09-27).
 */
class CurvedVertexConsumer(
    private val delegate: VertexConsumer,
    private val curve: Curve,
) : VertexConsumer {

    // The pending quad's four corners, attributes packed per corner at a fixed stride.
    private val poses: Array<Matrix4fc?> = arrayOfNulls(CORNERS)
    private val positions = FloatArray(CORNERS * 3)
    private val colors = FloatArray(CORNERS * 4)
    private val uvs = FloatArray(CORNERS * 2)
    private val overlays = IntArray(CORNERS * 2)
    private val lights = IntArray(CORNERS * 2)
    private val normals = FloatArray(CORNERS * 3)
    private var count: Int = 0

    private val current: Int get() = count - 1

    override fun addVertex(pose: Matrix4fc, x: Float, y: Float, z: Float): VertexConsumer = start(pose, x, y, z)

    // Already in world space, so there is nothing to bend: passed through with its quad unchanged.
    override fun addVertex(x: Float, y: Float, z: Float): VertexConsumer = start(null, x, y, z)

    override fun setColor(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer = apply {
        colors[current * 4] = red.toFloat()
        colors[current * 4 + 1] = green.toFloat()
        colors[current * 4 + 2] = blue.toFloat()
        colors[current * 4 + 3] = alpha.toFloat()
    }

    override fun setColor(argb: Int): VertexConsumer =
        setColor((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF, (argb ushr 24) and 0xFF)

    override fun setUv(u: Float, v: Float): VertexConsumer = apply {
        uvs[current * 2] = u
        uvs[current * 2 + 1] = v
    }

    override fun setUv1(u: Int, v: Int): VertexConsumer = apply {
        overlays[current * 2] = u
        overlays[current * 2 + 1] = v
    }

    override fun setUv2(u: Int, v: Int): VertexConsumer = apply {
        lights[current * 2] = u
        lights[current * 2 + 1] = v
    }

    override fun setNormal(x: Float, y: Float, z: Float): VertexConsumer = apply {
        normals[current * 3] = x
        normals[current * 3 + 1] = y
        normals[current * 3 + 2] = z
    }

    override fun setLineWidth(width: Float): VertexConsumer = this

    fun flush() {
        if (count == CORNERS) emit()
        count = 0
    }

    private fun start(pose: Matrix4fc?, x: Float, y: Float, z: Float): VertexConsumer {
        if (count == CORNERS) flush()
        poses[count] = pose
        positions[count * 3] = x
        positions[count * 3 + 1] = y
        positions[count * 3 + 2] = z
        colors.fill(255f, count * 4, count * 4 + 4)
        count++
        return this
    }

    private fun emit() {
        val across: Int = max(cuts(0, 1), cuts(3, 2))
        val down: Int = max(cuts(0, 3), cuts(1, 2))
        for (row in 0 until down) {
            val top: Float = row.toFloat() / down
            val bottom: Float = (row + 1).toFloat() / down
            for (column in 0 until across) {
                val left: Float = column.toFloat() / across
                val right: Float = (column + 1).toFloat() / across
                // Same order as the quad's own corners, so each cell keeps its winding.
                put(left, top)
                put(right, top)
                put(right, bottom)
                put(left, bottom)
            }
        }
    }

    private fun cuts(from: Int, to: Int): Int {
        val length: Float = hypot(positions[to * 3] - positions[from * 3], positions[to * 3 + 1] - positions[from * 3 + 1])
        return ceil(length * curve.unit / STEP).toInt().coerceAtLeast(1)
    }

    /** The vertex at ([s], [t]) across the quad: s runs corner 0 to 1 (and 3 to 2), t runs 0 to 3. */
    private fun put(s: Float, t: Float) {
        val x: Float = bilinear(positions, 3, 0, s, t)
        val y: Float = bilinear(positions, 3, 1, s, t)
        val z: Float = bilinear(positions, 3, 2, s, t)
        val pose: Matrix4fc? = poses[0]
        val consumer: VertexConsumer =
            if (pose == null) {
                delegate.addVertex(x, y, z)
            } else {
                val factor: Float = curve.factorAt(x, y)
                delegate.addVertex(pose, curve.along(x, factor), curve.along(y, factor), z)
            }
        consumer
            .setColor(
                bilinear(colors, 4, 0, s, t).toInt(),
                bilinear(colors, 4, 1, s, t).toInt(),
                bilinear(colors, 4, 2, s, t).toInt(),
                bilinear(colors, 4, 3, s, t).toInt(),
            )
            .setUv(bilinear(uvs, 2, 0, s, t), bilinear(uvs, 2, 1, s, t))
            .setUv1(overlays[0], overlays[1])
            .setUv2(lights[0], lights[1])
            .setNormal(normals[0], normals[1], normals[2])
    }

    private fun bilinear(values: FloatArray, stride: Int, offset: Int, s: Float, t: Float): Float {
        val a: Float = values[offset]
        val b: Float = values[stride + offset]
        val c: Float = values[2 * stride + offset]
        val d: Float = values[3 * stride + offset]
        val top: Float = a + (b - a) * s
        val bottom: Float = d + (c - d) * s
        return top + (bottom - top) * t
    }

    companion object {
        private const val CORNERS: Int = 4

        /** Longest side of a cell, on the face: short enough that a bent edge reads as a curve. */
        private const val STEP: Float = 1f / 32f
    }
}
