package com.xfastgames.witness.entities.renderer

import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
import com.xfastgames.witness.items.renderer.PuzzlePanelRenderer
import com.xfastgames.witness.screens.composer.PuzzleComposerScreen.Companion.PUZZLE_OUTPUT_SLOT_INDEX
import com.xfastgames.witness.utils.rotate
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.state.property.Properties
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3f

class PuzzleComposerBlockRenderer : BlockEntityRenderer<PuzzleComposerBlockEntity> {

    private val puzzlePanelRenderer = PuzzlePanelRenderer

    override fun render(
        blockEntity: PuzzleComposerBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumerProvider: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        matrices.push()

        // Get the relevant puzzle
        val itemStack: ItemStack = blockEntity.inventory.items[PUZZLE_OUTPUT_SLOT_INDEX]
        if (itemStack.isEmpty) return matrices.pop()

        // Move to center
        matrices.translate(.5, .815, .5)

        // Rotate the entity to the direction of the block
        val direction: Direction = blockEntity.cachedState.get(Properties.HORIZONTAL_FACING)
        matrices.rotate(Vec3f.POSITIVE_Y, -direction.asRotation())

        // Rotate to horizontal plane
        matrices.rotate(Vec3f.POSITIVE_X, 90.0f)

        // Scale the panel
        matrices.scale(0.85f, 0.85f, 1f)

        // Move slightly out of center to avoid z collision
        matrices.translate(-.475, -.475, -.125)

        // Scale to fit to frame
        matrices.scale(0.95f, 0.95f, 1f)

        // Get light above
        // TODO: Figure out lighting so that panel is lit properly
        val lightAbove: Int = WorldRenderer.getLightmapCoordinates(blockEntity.world, blockEntity.pos.up())

        // Render puzzle panel
        puzzlePanelRenderer.renderPanel(itemStack, matrices, vertexConsumerProvider, lightAbove, overlay)

        matrices.pop()
    }
}