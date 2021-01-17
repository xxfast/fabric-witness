package com.xfastgames.witness.entities.renderer

import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.renderer.PuzzlePanelRenderer
import com.xfastgames.witness.utils.rotate
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.item.ItemStack
import net.minecraft.state.property.Properties
import net.minecraft.util.math.Direction

class PuzzleFrameBlockRenderer(dispatcher: BlockEntityRenderDispatcher) :
    BlockEntityRenderer<PuzzleFrameBlockEntity>(dispatcher) {

    companion object {
        val PUZZLE_FRAME_SCALE = 0.85f
    }

    private val puzzlePanelRenderer = PuzzlePanelRenderer

    override fun render(
        blockEntity: PuzzleFrameBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumerProvider: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        matrices.push()

        // Get the relevant puzzle
        val itemStack: ItemStack = blockEntity.inventory.items[0]

        // Move to center
        matrices.translate(.5, .5, .5)

        // Rotate the entity to the direction of the block
        val direction: Direction = blockEntity.cachedState.get(Properties.HORIZONTAL_FACING)
        matrices.rotate(Vector3f.POSITIVE_Y, -direction.asRotation())

        // Scale the panel
        matrices.scale(PUZZLE_FRAME_SCALE, PUZZLE_FRAME_SCALE, 1f)

        // Move slightly out of center to avoid z collision
        matrices.translate(.0, .0, -.034)

        // Move to corner
        matrices.translate(-.5, -.5, -.05)

        if (itemStack.isEmpty) return matrices.pop()

        // Render puzzle panel
        puzzlePanelRenderer.renderPanel(itemStack, matrices, vertexConsumerProvider, light, overlay)
        matrices.pop()
    }
}