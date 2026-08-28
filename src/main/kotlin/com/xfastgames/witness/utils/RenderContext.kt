package com.xfastgames.witness.utils

import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.PoseStack
import org.joml.Vector3f

fun RenderContext.rectangle(position: Vector3f, width: Float, height: Float) =
    vertexConsumer.rectangle(entry, position, width, height, light, overlay)

fun RenderContext.square(
    position: Vector3f,
    length: Float,
    r: Float = 1f,
    g: Float = 1f,
    b: Float = 1f,
    a: Float = 1f,
) = vertexConsumer.square(entry, position, length, light, overlay, r, g, b, a)

fun RenderContext.circle(
    position: Vector3f,
    radius: Float,
    arc: IntRange = 0..360,
    r: Float = 1f,
    g: Float = 1f,
    b: Float = 1f,
    a: Float = 1f,
) = vertexConsumer.circle(entry, position, radius, light, overlay, arc, r = r, g = g, b = b, a = a)

fun RenderContext.ring(
    position: Vector3f,
    innerRadius: Float,
    outerRadius: Float,
    r: Float = 1f,
    g: Float = 1f,
    b: Float = 1f,
    a: Float = 1f,
) = vertexConsumer.ring(entry, position, innerRadius, outerRadius, light, overlay, r, g, b, a)

/**
 * A regular hexagon, point up, [radius] from centre to point.
 *
 * A six step sweep of [circle] is already exactly a hexagon, so this needs no new geometry: the arc
 * starts at the top (`sin`/`cos` of 0), which puts a point up rather than a flat.
 */
fun RenderContext.hexagon(
    position: Vector3f,
    radius: Float,
    r: Float = 1f,
    g: Float = 1f,
    b: Float = 1f,
    a: Float = 1f,
) = vertexConsumer.circle(
    entry, position, radius, light, overlay,
    arc = 0..360,
    resolution = 60.0,
    r = r, g = g, b = b, a = a,
)

fun RenderContext.line(start: Vector3f, end: Vector3f, thickness: Float) =
    vertexConsumer.line(entry, start, end, thickness, light, overlay)

fun withRenderContext(
    entry: PoseStack.Pose,
    vertexConsumer: VertexConsumer,
    light: Int,
    overlay: Int,
    block: RenderContext.() -> Unit
) = with(RenderContext(entry, vertexConsumer, light, overlay), block)

data class RenderContext(
    val entry: PoseStack.Pose,
    val vertexConsumer: VertexConsumer,
    val light: Int,
    val overlay: Int
)
