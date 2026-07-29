package com.xfastgames.witness.utils

import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.util.math.MatrixStack
import org.joml.Vector3f

fun RenderContext.rectangle(position: Vector3f, width: Float, height: Float) =
    vertexConsumer.rectangle(entry, position, width, height, light, overlay)

fun RenderContext.square(position: Vector3f, length: Float) =
    vertexConsumer.square(entry, position, length, light, overlay)

fun RenderContext.circle(position: Vector3f, radius: Float, arc: IntRange = 0..360) =
    vertexConsumer.circle(entry, position, radius, light, overlay, arc)

/**
 * A regular hexagon, point up, [radius] from centre to point.
 *
 * A six step sweep of [circle] is already exactly a hexagon, so this needs no new geometry: the arc
 * starts at the top (`sin`/`cos` of 0), which puts a point up rather than a flat.
 */
fun RenderContext.hexagon(position: Vector3f, radius: Float) =
    vertexConsumer.circle(entry, position, radius, light, overlay, 0..360, resolution = 60.0)

fun RenderContext.line(start: Vector3f, end: Vector3f, thickness: Float) =
    vertexConsumer.line(entry, start, end, thickness, light, overlay)

fun withRenderContext(
    entry: MatrixStack.Entry,
    vertexConsumer: VertexConsumer,
    light: Int,
    overlay: Int,
    block: RenderContext.() -> Unit
) = with(RenderContext(entry, vertexConsumer, light, overlay), block)

data class RenderContext(
    val entry: MatrixStack.Entry,
    val vertexConsumer: VertexConsumer,
    val light: Int,
    val overlay: Int
)
