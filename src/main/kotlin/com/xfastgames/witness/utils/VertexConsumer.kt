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

fun VertexConsumer.line(
    matrices: MatrixStack,
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
        start.copy().apply { add(0f, -halfThickness, 0f) },
        start.copy().apply { add(0f, halfThickness, 0f) },
        start.copy().apply { add(length - thickness, +halfThickness, 0f) },
        start.copy().apply { add(length - thickness, -halfThickness, 0f) },
    ).map { corner ->
        val tempX: Float = corner.x - start.x
        val tempY: Float = corner.y - start.y
        val rotatedX: Float = tempX * cos(theta) - tempY * sin(theta)
        val rotatedY: Float = tempX * sin(theta) + tempY * cos(theta)
        Vector3f(rotatedX + start.x, rotatedY + start.y, start.z)
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



