package com.xfastgames.witness.utils

import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.util.math.MatrixStack
import org.joml.Matrix4f
import org.joml.Vector3f
import java.lang.Math.toRadians
import kotlin.math.*

// 1.21 VertexConsumer: positions use org.joml matrices, normals take the MatrixStack.Entry directly,
// and vertices auto-advance (the old `.next()` terminator was removed). These helpers operate on a
// captured MatrixStack.Entry because the render-command-queue hands renderers an entry, not a stack.

fun VertexConsumer.circle(
    entry: MatrixStack.Entry,
    center: Vector3f,
    radius: Float,
    light: Int,
    overlay: Int,
    arc: IntRange = 0..360,
    resolution: Double = 15.0,
    r: Float = 1f,
    g: Float = 1f,
    b: Float = 1f,
    a: Float = 1f,
) {
    val model: Matrix4f = entry.positionMatrix

    var theta: Double = arc.first + resolution
    while (theta < arc.last) {
        theta -= resolution
        // Anchor quad to center
        vertex(model, center.x, center.y, center.z)
            .color(r, g, b, a)
            .texture(0f, 1f)
            .overlay(overlay)
            .light(light)
            .normal(entry, .5f, .5f, .5f)
        // Draw quad segments
        repeat(3) {
            vertex(
                model,
                center.x + radius * sin(toRadians(theta).toFloat()),
                center.y + radius * cos(toRadians(theta).toFloat()),
                center.z
            )
                .color(r, g, b, a)
                .texture(0f, 1f)
                .overlay(overlay)
                .light(light)
                .normal(entry, .5f, .5f, .5f)
            theta += resolution
        }
    }
}

/**
 * Annulus between [innerRadius] and [outerRadius]. Used for the tutorial attract pulse ring
 * (Witness-style expanding white hint on start / end nodes).
 */
fun VertexConsumer.ring(
    entry: MatrixStack.Entry,
    center: Vector3f,
    innerRadius: Float,
    outerRadius: Float,
    light: Int,
    overlay: Int,
    r: Float = 1f,
    g: Float = 1f,
    b: Float = 1f,
    a: Float = 1f,
    resolution: Double = 8.0
) {
    if (a <= 0f || outerRadius <= 0f || outerRadius <= innerRadius) return
    val model: Matrix4f = entry.positionMatrix
    val inner: Float = innerRadius.coerceAtLeast(0f)

    fun vert(radius: Float, thetaRad: Float) {
        vertex(
            model,
            center.x + radius * sin(thetaRad),
            center.y + radius * cos(thetaRad),
            center.z
        )
            .color(r, g, b, a)
            .texture(0f, 1f)
            .overlay(overlay)
            .light(light)
            .normal(entry, .5f, .5f, .5f)
    }

    var theta: Double = 0.0
    while (theta < 360.0) {
        val t0: Float = toRadians(theta).toFloat()
        val t1: Float = toRadians(theta + resolution).toFloat()
        // Outer → outer → inner → inner, matching the quad winding used elsewhere.
        vert(outerRadius, t0)
        vert(outerRadius, t1)
        vert(inner, t1)
        vert(inner, t0)
        theta += resolution
    }
}

fun VertexConsumer.square(entry: MatrixStack.Entry, position: Vector3f, length: Float, light: Int, overlay: Int) {
    val offSets: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f).reversed()
    offSets.forEach { (offsetX, offsetY) ->
        val model: Matrix4f = entry.positionMatrix
        vertex(model, position.x + offsetX * length, position.y + offsetY * length, position.z)
            .color(1f, 1f, 1f, 1f)
            .texture(offsetX, offsetY)
            .overlay(overlay)
            .light(light)
            .normal(entry, 1f, 1f, 1f)
    }
}

fun VertexConsumer.rectangle(
    entry: MatrixStack.Entry,
    position: Vector3f,
    width: Float,
    height: Float,
    light: Int,
    overlay: Int
) {
    val offSets: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f).reversed()
    offSets.forEach { (offsetX, offsetY) ->
        val model: Matrix4f = entry.positionMatrix
        vertex(model, position.x + offsetX * width, position.y + offsetY * height, position.z)
            .color(1f, 1f, 1f, 1f)
            .texture(offsetX, offsetY)
            .overlay(overlay)
            .light(light)
            .normal(entry, .5f, .5f, .5f)
    }
}

fun VertexConsumer.line(
    entry: MatrixStack.Entry,
    u: Vector3f,
    v: Vector3f,
    thickness: Float,
    light: Int,
    overlay: Int
) {
    val max: Vector3f = maxOf(u, v)
    val min: Vector3f = minOf(u, v)
    val theta: Float = atan2(u.y - v.y, u.x - v.x)
    val halfThickness: Float = thickness / 2
    val lengthX: Float = u.x - v.x
    val lengthY: Float = u.y - v.y
    val length: Float = sqrt(lengthX.pow(2) + lengthY.pow(2)) + thickness

    val start: Vector3f = if (theta > 0f) min else max
    val vertices: List<Vector3f> = listOf(
        Vector3f(start).add(0f, -halfThickness, 0f),
        Vector3f(start).add(0f, halfThickness, 0f),
        Vector3f(start).add(length - thickness, +halfThickness, 0f),
        Vector3f(start).add(length - thickness, -halfThickness, 0f),
    ).map { corner ->
        val tempX: Float = corner.x - start.x
        val tempY: Float = corner.y - start.y
        val rotatedX: Float = tempX * cos(theta) - tempY * sin(theta)
        val rotatedY: Float = tempX * sin(theta) + tempY * cos(theta)
        Vector3f(rotatedX + start.x, rotatedY + start.y, start.z)
    }

    vertices.forEach { position ->
        val model: Matrix4f = entry.positionMatrix
        vertex(model, position.x, position.y, position.z)
            .color(1f, 1f, 1f, 1f)
            .texture(0f, 1f)
            .overlay(overlay)
            .light(light)
            .normal(entry, .5f, .5f, .5f)
    }
}
