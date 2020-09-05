package com.xfastgames.witness.utils

import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.util.math.Matrix3f
import net.minecraft.util.math.Matrix4f
import java.lang.Math.toRadians
import kotlin.math.*

fun VertexConsumer.circle(
    matrices: MatrixStack,
    center: Vector3f,
    radius: Float,
    light: Int,
    overlay: Int,
    arc: IntRange = 0..360,
    resolution: Double = 15.0
) {
    val matrix: MatrixStack.Entry = matrices.peek()
    val normal: Matrix3f = matrix.normal
    val model: Matrix4f = matrix.model

    var theta: Double = arc.first + resolution
    while (theta < arc.last) {
        theta -= resolution
        // Anchor quad to center
        vertex(model, center.x, center.y, center.z)
        color(1f, 1f, 1f, 1f)
        texture(0f, 1f)
        overlay(overlay)
        light(light)
        normal(normal, .5f, .5f, .5f)
        next()
        // Draw quad segments
        repeat(3) {
            vertex(
                model,
                center.x + radius * sin(toRadians(theta).toFloat()),
                center.y + radius * cos(toRadians(theta).toFloat()),
                center.z
            )
            color(1f, 1f, 1f, 1f)
            texture(0f, 1f)
            overlay(overlay)
            light(light)
            normal(normal, .5f, .5f, .5f)
            next()
            theta += resolution
        }
    }
}

fun VertexConsumer.square(matrices: MatrixStack, position: Vector3f, length: Float, light: Int, overlay: Int) {
    val offSets: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f).reversed()
    offSets.forEach { (offsetX, offsetY) ->
        val matrix: MatrixStack.Entry = matrices.peek()
        val normal: Matrix3f = matrix.normal
        val model: Matrix4f = matrix.model
        vertex(model, position.x + offsetX * length, position.y + offsetY * length, position.z)
        color(1f, 1f, 1f, 1f)
        texture(offsetX, offsetY)
        overlay(overlay)
        light(light)
        normal(normal, 1f, 1f, 1f)
        next()
    }
}

fun VertexConsumer.rectangle(
    matrices: MatrixStack,
    position: Vector3f,
    width: Float,
    height: Float,
    light: Int,
    overlay: Int
) {
    val offSets: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f).reversed()
    offSets.forEach { (offsetX, offsetY) ->
        val matrix: MatrixStack.Entry = matrices.peek()
        val normal: Matrix3f = matrix.normal
        val model: Matrix4f = matrix.model
        vertex(model, position.x + offsetX * width, position.y + offsetY * height, position.z)
        color(1f, 1f, 1f, 1f)
        texture(offsetX, offsetY)
        overlay(overlay)
        light(light)
        normal(normal, .5f, .5f, .5f)
        next()
    }
}

/**
 * Currently broken / not working as intended because i'm bad at math 😛
 */
fun VertexConsumer.anyLine(
    matrices: MatrixStack,
    start: Vector3f,
    end: Vector3f,
    thickness: Float,
    light: Int,
    overlay: Int
) {
    val theta: Float = atan2(end.y - start.y, end.x - start.x)
    val delta: Double = theta - toRadians(135.0)
    val hypotenuse: Double = sqrt((thickness / 2.0).pow(2.0) * 2)
    val adjacent: Float = (cos(delta) * hypotenuse).toFloat()
    val opposite: Float = (sin(delta) * hypotenuse).toFloat()
    val r1: Vector3f = start.copy().apply { add(opposite, -adjacent, 0f) }
    val r2: Vector3f = start.copy().apply { add(-adjacent, opposite, 0f) }
    val r3: Vector3f = end.copy().apply { add(-opposite, adjacent, 0f) }
    val r4: Vector3f = end.copy().apply { add(adjacent, -opposite, 0f) }
    val vertices: List<Vector3f> = listOf(r1, r2, r3, r4)
    vertices.forEach { position ->
        val matrix: MatrixStack.Entry = matrices.peek()
        val normal: Matrix3f = matrix.normal
        val model: Matrix4f = matrix.model
        vertex(model, position.x, position.y, position.z)
        color(1f, 1f, 1f, 1f)
        texture(0f, 1f)
        overlay(overlay)
        light(light)
        normal(normal, .5f, .5f, .5f)
        next()
    }
}


/**
 * Currently only working for non-diagonal lines. See [anyLine]
 */
fun VertexConsumer.line(
    matrices: MatrixStack,
    start: Vector3f,
    end: Vector3f,
    thickness: Float,
    light: Int,
    overlay: Int
) {
    val halfThickness: Float = thickness / 2
    val isHorizontal: Boolean = start.y == end.y && start.x != end.x
    val isVertical: Boolean = start.x == end.x && start.y != end.y
    val vertices: List<Vector3f> = when {
        isHorizontal -> {
            val xMax: Vector3f = if (start.x > end.x) start else end
            val xMin: Vector3f = if (start.x < end.x) start else end
            val r1: Vector3f = xMax.copy().apply { add(halfThickness, -halfThickness, 0f) }
            val r2: Vector3f = xMax.copy().apply { add(halfThickness, halfThickness, 0f) }
            val r3: Vector3f = xMin.copy().apply { add(-halfThickness, halfThickness, 0f) }
            val r4: Vector3f = xMin.copy().apply { add(-halfThickness, -halfThickness, 0f) }
            listOf(r4, r3, r2, r1)
        }

        isVertical -> {
            val yMin: Vector3f = if (start.y < end.y) start else end
            val yMax: Vector3f = if (start.y > end.y) start else end
            val r1: Vector3f = yMin.copy().apply { add(halfThickness, -halfThickness, 0f) }
            val r2: Vector3f = yMax.copy().apply { add(halfThickness, halfThickness, 0f) }
            val r3: Vector3f = yMax.copy().apply { add(-halfThickness, halfThickness, 0f) }
            val r4: Vector3f = yMin.copy().apply { add(-halfThickness, -halfThickness, 0f) }
            listOf(r4, r3, r2, r1)
        }

        else -> throw IllegalArgumentException("Only horizontal or diagonals allowed")
    }

    vertices.forEach { position ->
        val matrix: MatrixStack.Entry = matrices.peek()
        val normal: Matrix3f = matrix.normal
        val model: Matrix4f = matrix.model
        vertex(model, position.x, position.y, position.z)
        color(1f, 1f, 1f, 1f)
        texture(0f, 1f)
        overlay(overlay)
        light(light)
        normal(normal, .5f, .5f, .5f)
        next()
    }
}



