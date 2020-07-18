package com.xfastgames.witness.items.renderer

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.Direction
import com.xfastgames.witness.items.Line
import com.xfastgames.witness.items.Tile
import com.xfastgames.witness.utils.*
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRenderer
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

class PuzzleTileItemRenderer : BuiltinItemRenderer {

    private val fillTexture = Identifier(Witness.IDENTIFIER, "textures/entity/puzzle_panel_fill.png")

    override fun render(
        stack: ItemStack,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        matrices.push()

        val tile: Tile = stack.tag?.let { Tile.fromTag(it) } ?: return matrices.pop()

        val position = Vector3f(0f, 0f, -0.001f)

        // Render tile pieces
        val fillConsumer: VertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(fillTexture))

        fun rectangle(position: Vector3f, width: Float, height: Float) =
            fillConsumer.rectangle(matrices, position, width, height, light, overlay)

        fun square(position: Vector3f, length: Float) =
            fillConsumer.square(matrices, position, length, light, overlay)

        fun circle(position: Vector3f, radius: Float, arc: IntRange = 0..360) =
            fillConsumer.circle(matrices, position, radius, light, overlay, arc)

        tile.lines.forEach { (direction, side) ->
            when (direction) {
                Direction.TOP -> when (side) {
                    Line.FILLED -> rectangle(Vector3f(6.pc, 0.pc, position.z), 4.pc, 6.pc)
                    Line.SHORTENED -> rectangle(Vector3f(6.pc, 2.pc, position.z), 4.pc, 4.pc)
                    Line.END -> {
                        circle(Vector3f(8.pc, 4.pc, position.z), 2.pc, arc = 90..270)
                        rectangle(Vector3f(6.pc, 4.pc, position.z), 4.pc, 2.pc)
                    }
                }

                Direction.RIGHT -> when (side) {
                    Line.FILLED -> rectangle(Vector3f(10.pc, 6.pc, position.z), 6.pc, 4.pc)
                    Line.SHORTENED -> rectangle(Vector3f(10.pc, 6.pc, position.z), 4.pc, 4.pc)
                    Line.END -> {
                        circle(Vector3f(12.pc, 8.pc, position.z), 2.pc, 0..180)
                        rectangle(Vector3f(10.pc, 6.pc, position.z), 2.pc, 4.pc)
                    }
                }

                Direction.BOTTOM -> when (side) {
                    Line.FILLED -> rectangle(Vector3f(6.pc, 10.pc, position.z), 4.pc, 6.pc)
                    Line.SHORTENED -> rectangle(Vector3f(6.pc, 10.pc, position.z), 4.pc, 4.pc)
                    Line.END -> {
                        circle(Vector3f(8.pc, 12.pc, position.z), 2.pc, arc = -90..90)
                        rectangle(Vector3f(6.pc, 10.pc, position.z), 4.pc, 2.pc)
                    }
                }

                Direction.LEFT -> when (side) {
                    Line.FILLED -> rectangle(Vector3f(0.pc, 6.pc, position.z), 6.pc, 4.pc)
                    Line.SHORTENED -> rectangle(Vector3f(2.pc, 6.pc, position.z), 4.pc, 4.pc)
                    Line.END -> {
                        circle(Vector3f(4.pc, 8.pc, position.z), 2.pc, 180..360)
                        rectangle(Vector3f(4.pc, 6.pc, position.z), 2.pc, 4.pc)
                    }
                }
            }
        }

        if (tile.isStart) circle(Vector3f(8.pc, 8.pc, position.z), 5.pc)
        else when {
            tile.center.containsOnly(Direction.TOP, Direction.LEFT) -> {
                square(Vector3f(6.pc, 6.pc, position.z), 2.pc)
                square(Vector3f(8.pc, 6.pc, position.z), 2.pc)
                square(Vector3f(6.pc, 8.pc, position.z), 2.pc)
                circle(Vector3f(8.pc, 8.pc, position.z), 2.pc, 0..90)
            }

            tile.center.containsOnly(Direction.TOP, Direction.RIGHT) -> {
                square(Vector3f(6.pc, 6.pc, position.z), 2.pc)
                square(Vector3f(8.pc, 6.pc, position.z), 2.pc)
                square(Vector3f(8.pc, 8.pc, position.z), 2.pc)
                circle(Vector3f(8.pc, 8.pc, position.z), 2.pc, 270..360)
            }

            tile.center.containsOnly(Direction.BOTTOM, Direction.LEFT) -> {
                square(Vector3f(6.pc, 6.pc, position.z), 2.pc)
                square(Vector3f(8.pc, 8.pc, position.z), 2.pc)
                square(Vector3f(6.pc, 8.pc, position.z), 2.pc)
                circle(Vector3f(8.pc, 8.pc, position.z), 2.pc, 90..180)
            }

            tile.center.containsOnly(Direction.BOTTOM, Direction.RIGHT) -> {
                square(Vector3f(8.pc, 6.pc, position.z), 2.pc)
                square(Vector3f(6.pc, 8.pc, position.z), 2.pc)
                square(Vector3f(8.pc, 8.pc, position.z), 2.pc)
                circle(Vector3f(8.pc, 8.pc, position.z), 2.pc, 180..270)
            }

            else -> square(Vector3f(6.pc, 6.pc, position.z), 4.pc)
        }

        matrices.pop()
    }
}