package com.xfastgames.witness.utils

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Matrix4f
import kotlin.math.cos
import kotlin.math.sin

fun DrawableHelper.fill(
    matrices: MatrixStack,
    x1: Int,
    y1: Int,
    x2: Int,
    y2: Int,
    r: Float,
    g: Float,
    b: Float,
    a: Float
) {
    val matrix: Matrix4f = matrices.peek().model
    val bufferBuilder: BufferBuilder = Tessellator.getInstance().buffer
    RenderSystem.enableBlend()
    RenderSystem.disableTexture()
    RenderSystem.defaultBlendFunc()
    bufferBuilder.begin(7, VertexFormats.POSITION_COLOR)
    bufferBuilder.vertex(matrix, x1.toFloat(), y2.toFloat(), 0.0f).color(r, g, b, a).next()
    bufferBuilder.vertex(matrix, x2.toFloat(), y2.toFloat(), 0.0f).color(r, g, b, a).next()
    bufferBuilder.vertex(matrix, x2.toFloat(), y1.toFloat(), 0.0f).color(r, g, b, a).next()
    bufferBuilder.vertex(matrix, x1.toFloat(), y1.toFloat(), 0.0f).color(r, g, b, a).next()
    bufferBuilder.end()
    BufferRenderer.draw(bufferBuilder)
    RenderSystem.enableTexture()
    RenderSystem.disableBlend()
}

fun DrawableHelper.circle(
    matrices: MatrixStack, centerX: Int, centerY: Int, radius: Int,
    r: Float, g: Float, b: Float, a: Float,
    arc: IntRange = 0..360,
    resolution: Double = 15.0
) {
    val matrix: Matrix4f = matrices.peek().model
    val bufferBuilder: BufferBuilder = Tessellator.getInstance().buffer
    RenderSystem.enableBlend()
    RenderSystem.disableTexture()
    RenderSystem.defaultBlendFunc()
    bufferBuilder.begin(7, VertexFormats.POSITION_COLOR)

    var theta: Double = arc.first + resolution
    while (theta < arc.last) {
        theta -= resolution
        // Anchor quad to center
        bufferBuilder.vertex(matrix, centerX.toFloat(), centerY.toFloat(), 0.0f).color(r, g, b, a).next()
        // Draw quad segments
        repeat(3) {
            bufferBuilder
                .vertex(
                    matrix,
                    centerX + radius * sin(Math.toRadians(theta).toFloat()),
                    centerY + radius * cos(Math.toRadians(theta).toFloat()),
                    0.0f
                )
                .color(r, g, b, a)
                .next()
            theta += resolution
        }
    }

    bufferBuilder.end()
    BufferRenderer.draw(bufferBuilder)
    RenderSystem.enableTexture()
    RenderSystem.disableBlend()
}

