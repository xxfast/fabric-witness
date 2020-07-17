package com.xfastgames.witness.items.renderer

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.Panel
import com.xfastgames.witness.items.PuzzlePanel
import com.xfastgames.witness.items.PuzzleTile
import com.xfastgames.witness.utils.pc
import com.xfastgames.witness.utils.square
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRenderer
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

class PuzzlePanelItemRenderer : BuiltinItemRenderer {

    private val backdropTexture = Identifier(Witness.IDENTIFIER, "textures/entity/puzzle_panel_backdrop.png")

    private val tileRenderer: PuzzleTileItemRenderer = PuzzleTile.RENDERER

    val pX: (Int) -> List<List<Boolean>> = { size: Int ->
        val col = mutableListOf<List<Boolean>>()
        repeat(size) { x ->
            val row = mutableListOf<Boolean>()
            repeat(size) { y ->
                row.add(true)
            }
            col.add(row.toList())
        }
        col.toList()
    }


    override fun render(
        stack: ItemStack,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        matrices.push()

        val backdropConsumer: VertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(backdropTexture))

        matrices.translate(.0, .0, .470)
        backdropConsumer.square(matrices, Vector3f(0.pc, 0.pc, 0.pc), 16.pc, light, overlay)

        val puzzle: Panel = stack.tag?.let { PuzzlePanel.fromTag(it) } ?: return matrices.pop()

        // Scale items to fit on frame
        val xCount: Int = puzzle.tiles.size
        val yCount: Int = puzzle.tiles.map { it.size }.max()!!

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
        puzzle.tiles.forEachIndexed { x, row ->
            val dX: Double = x * (xScale.toDouble() * xCount) - ((xScale * xCount) * xScaledOffset)
            matrices.translate(dX, .0, .0)
            row.forEachIndexed { y, tile ->
                val dY: Double = y * (yScale.toDouble() * yCount) - ((yScale * yCount) * yScaledOffset)
                matrices.translate(.0, dY, .0)
                val tileStack: ItemStack = ItemStack(PuzzleTile.ITEM, 1).apply { tag = PuzzleTile.toTag(tile) }
                tileRenderer.render(tileStack, matrices, vertexConsumers, light, overlay)
                matrices.translate(.0, -dY, .0)
            }
            matrices.translate(-dX, .0, .0)
        }
        matrices.scale(1 + xScale, 1 + yScale, 1f)

        matrices.pop()
    }
}