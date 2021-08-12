package com.xfastgames.witness.utils

import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec3f

fun RenderContext.rectangle(position: Vec3f, width: Float, height: Float) =
    vertexConsumer.rectangle(matrices, position, width, height, light, overlay)

fun RenderContext.square(position: Vec3f, length: Float) =
    vertexConsumer.square(matrices, position, length, light, overlay)

fun RenderContext.circle(position: Vec3f, radius: Float, arc: IntRange = 0..360) =
    vertexConsumer.circle(matrices, position, radius, light, overlay, arc)

fun RenderContext.line(start: Vec3f, end: Vec3f, thickness: Float) =
    vertexConsumer.line(matrices, start, end, thickness, light, overlay)

fun withRenderContext(
    matrices: MatrixStack,
    vertexConsumer: VertexConsumer,
    light: Int,
    overlay: Int,
    block: RenderContext.() -> Unit
) = with(RenderContext(matrices, vertexConsumer, light, overlay), block)

data class RenderContext(
    val matrices: MatrixStack,
    val vertexConsumer: VertexConsumer,
    val light: Int,
    val overlay: Int
)
