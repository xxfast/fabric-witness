package com.xfastgames.witness.items.renderer

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.utils.*
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRenderer
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier

object PuzzlePanelRenderer : BuiltinItemRenderer {

    private val itemModel = ModelIdentifier(PuzzlePanelItem.IDENTIFIER.toString())
    private val backdropTexture = Identifier(Witness.IDENTIFIER, "textures/entity/puzzle_panel_backdrop.png")
    private val lineFillTexture = Identifier(Witness.IDENTIFIER, "textures/entity/puzzle_panel_line_fill.png")
    private val solutionFillTexture = Identifier(Witness.IDENTIFIER, "textures/entity/puzzle_panel_solution_fill.png")

    override fun render(
        stack: ItemStack,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        matrices.translate(1.0, .0, .5)
        matrices.rotate(Vector3f.NEGATIVE_Y, 180f)
        renderPanel(stack, matrices, vertexConsumers, light, overlay)
    }

    /** Pixel coordinates */
    private val Int.pc: Float get() = (1f / 16f) * this

    fun renderPanel(
        stack: ItemStack,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        matrices.push()

        // Render Panel background
        val backdropConsumer: VertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(backdropTexture))
        backdropConsumer.square(matrices, Vector3f(0.pc, 0.pc, 0.pc), 16.pc, light, overlay)

        // Retrieve panel to render
        val tag: CompoundTag = stack.tag.takeIf { stack.item == PuzzlePanelItem.ITEM } ?: return matrices.pop()
        val puzzle: Panel = tag.getPanel()

        // Rotate if handheld
        if (stack.holder is PlayerEntity) matrices.rotate(Vector3f.POSITIVE_Z, 180f)

        // Scale items to fit on frame
        val xCount: Int = puzzle.tiles.size
        val yCount: Int = puzzle.tiles.map { it.size }.max() ?: 0

        val xScale: Float = 1f / xCount
        val yScale: Float = 1f / yCount

        // Move to frame
        val xScaledOffset: Double = (xCount.toDouble() / 2) - 0.5
        val yScaledOffset: Double = (yCount.toDouble() / 2) - 0.5

        matrices.translate(
            (xScale.toDouble() * xCount / 2) - xScale / 2,
            (yScale.toDouble() * yCount / 2) - yScale / 2,
            .0
        )

        matrices.scale(xScale, yScale, 1f)

        // Render tiles
        puzzle.tiles.forEachIndexed { x, row ->
            val dX: Double = x * (xScale.toDouble() * xCount) - ((xScale * xCount) * xScaledOffset)
            matrices.translate(dX, .0, .0)
            row.forEachIndexed { y, tile ->
                val dY: Double = y * (yScale.toDouble() * yCount) - ((yScale * yCount) * yScaledOffset)
                matrices.translate(.0, dY, .0)
                renderTile(tile, matrices, vertexConsumers, light, overlay)
                matrices.translate(.0, -dY, .0)
            }
            matrices.translate(-dX, .0, .0)
        }
        matrices.scale(1 + xScale, 1 + yScale, 1f)
        matrices.pop()
    }

    private fun renderTile(
        tile: Tile,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        matrices.push()

        val position = Vector3f(0f, 0f, -0.001f)

        // Render tile pieces
        val fillConsumer: VertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(lineFillTexture))

        // Render tile lines
        withRenderContext(matrices, fillConsumer, light, overlay) {
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

            // Render tile centers or the vertex
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

                tile.center.isEmpty() -> {
                }

                else -> square(Vector3f(6.pc, 6.pc, position.z), 4.pc)
            }
        }
        matrices.pop()
    }

    fun renderLine(
        stack: ItemStack,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        matrices.push()
        val tag: CompoundTag = stack.tag.takeIf { stack.item == PuzzlePanelItem.ITEM } ?: return matrices.pop()
        val puzzle: Panel = tag.getPanel()
        if (puzzle.line.isEmpty()) return matrices.pop()

        // Scale items to fit on frame
        val xCount: Int = puzzle.tiles.size
        val yCount: Int = puzzle.tiles.map { it.size }.max() ?: 0

        val xScale: Float = 1f / xCount
        val yScale: Float = 1f / yCount

        // Move to frame
        val xScaledOffset: Double = (xCount.toDouble() / 2) - 0.5
        val yScaledOffset: Double = (yCount.toDouble() / 2) - 0.5
        matrices.translate(
            (xScale.toDouble() * xCount / 2) - xScale / 2,
            (yScale.toDouble() * yCount / 2) - yScale / 2,
            .0
        )

        matrices.scale(xScale, yScale, 1f)

        // Render line
        val line: List<Float> = listOf(.0f, .0f)
        val consumer: VertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(solutionFillTexture))
        val position = Vector3f(0f, 0f, -0.002f)
        withRenderContext(matrices, consumer, light, overlay) {
            val coordinates: List<Pair<Float, Float>> = line.chunked(2).map { (x, y) -> x to y }
            val (startX, startY) = coordinates.first()
            val dX: Double = startX * (xScale.toDouble() * xCount) - ((xScale * xCount) * xScaledOffset)
            val dY: Double = startY * (yScale.toDouble() * yCount) - ((yScale * yCount) * yScaledOffset)
            matrices.translate(dX, .0, .0)
            matrices.translate(.0, dY, .0)
            circle(Vector3f(8.pc, 8.pc, position.z), 5.pc)
        }
        matrices.scale(1 + xScale, 1 + yScale, 1f)
        matrices.pop()
    }
}