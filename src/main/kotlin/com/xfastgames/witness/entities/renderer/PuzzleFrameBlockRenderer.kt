package com.xfastgames.witness.entities.renderer

import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.PuzzleTile
import com.xfastgames.witness.utils.rotate
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.item.ItemRenderer
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.state.property.Properties
import net.minecraft.util.math.Direction

class PuzzleFrameBlockRenderer(dispatcher: BlockEntityRenderDispatcher) :
    BlockEntityRenderer<PuzzleFrameBlockEntity>(dispatcher) {

    private val backgroundStack = ItemStack(Items.YELLOW_STAINED_GLASS_PANE, 1)
    private val foregroundStack = ItemStack(PuzzleTile.ITEM, 1)
    private val itemRenderer: ItemRenderer = MinecraftClient.getInstance().itemRenderer

    override fun render(
        blockEntity: PuzzleFrameBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumerProvider: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
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

        val puzzle = pX(4)

        matrices.push()

        // Move to center
        matrices.translate(.5, .5, .5)

        // Rotate the entity to the direction of the block
        val direction: Direction = blockEntity.cachedState.get(Properties.HORIZONTAL_FACING)
        matrices.rotate(Vector3f.POSITIVE_Y, -direction.asRotation())

        // Move slightly out of center to avoid z collision
        matrices.translate(.0, .0, -.1)

        val frameScaleReduction = 0.15f

        // Scale
        matrices.scale(1 - frameScaleReduction, 1 - frameScaleReduction, 1 - frameScaleReduction)

        // Render Backdrop
        itemRenderer.renderItem(
            backgroundStack,
            ModelTransformation.Mode.GUI,
            light,
            overlay,
            matrices,
            vertexConsumerProvider
        )

        // Scale items to fit on frame
        val xCount: Int = puzzle.size
        val yCount: Int = puzzle.map { it.size }.max()!!

        val xScale: Float = 1f / xCount
        val yScale: Float = 1f / yCount

        // Move to front
        matrices.translate(.0, .0, -.035)

        // Rotate the frame right way up
        matrices.rotate(Vector3f.POSITIVE_Z, 180f)

        // Move to frame
        val xScaledOffset: Double = (xCount.toDouble() / 2) - 0.5
        val yScaledOffset: Double = (yCount.toDouble() / 2) - 0.5

        matrices.scale(xScale, yScale, .1f)
        puzzle.forEachIndexed { x, row ->
            val dX: Double = x * (xScale.toDouble() * xCount) - ((xScale * xCount) * xScaledOffset)
            matrices.translate(dX, .0, .0)
            row.forEachIndexed { y, cell ->
                val dY: Double = y * (yScale.toDouble() * yCount) - ((yScale * yCount) * yScaledOffset)
                matrices.translate(.0, dY, .0)
                val lightAbove =
                    WorldRenderer.getLightmapCoordinates(blockEntity.world, blockEntity.pos.up())
                if (cell) itemRenderer.renderItem(
                    foregroundStack,
                    ModelTransformation.Mode.GUI,
                    lightAbove,
                    overlay,
                    matrices,
                    vertexConsumerProvider
                )
                matrices.translate(.0, -dY, .0)
            }
            matrices.translate(-dX, .0, .0)
        }
        matrices.scale(1 + xScale, 1 + yScale, 1.0f)

        matrices.pop()
    }
}