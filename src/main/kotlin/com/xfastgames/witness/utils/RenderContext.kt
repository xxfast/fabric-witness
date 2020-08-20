package com.xfastgames.witness.utils

import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f

fun RenderContext.rectangle(position: Vector3f, width: Float, height: Float) =
    vertexConsumer.rectangle(matrices, position, width, height, light, overlay)

fun RenderContext.square(position: Vector3f, length: Float) =
    vertexConsumer.square(matrices, position, length, light, overlay)

fun RenderContext.circle(position: Vector3f, radius: Float, arc: IntRange = 0..360) =
    vertexConsumer.circle(matrices, position, radius, light, overlay, arc)

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
