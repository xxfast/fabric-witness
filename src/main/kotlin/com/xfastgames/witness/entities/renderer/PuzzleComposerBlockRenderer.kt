package com.xfastgames.witness.entities.renderer

import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.panel
import com.xfastgames.witness.items.renderer.PuzzlePanelRenderer
import com.xfastgames.witness.screens.composer.PuzzleComposerScreen.Companion.PUZZLE_OUTPUT_SLOT_INDEX
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState
import net.minecraft.client.render.command.ModelCommandRenderer
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.state.CameraRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.state.property.Properties
import net.minecraft.util.math.Direction
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d

class PuzzleComposerRenderState : BlockEntityRenderState() {
    var panel: Panel? = null
    var facing: Direction = Direction.NORTH
    var lightAbove: Int = 0
}

class PuzzleComposerBlockRenderer : BlockEntityRenderer<PuzzleComposerBlockEntity, PuzzleComposerRenderState> {

    companion object {
        fun register() {
            BlockEntityRendererFactories.register(PuzzleComposerBlockEntity.ENTITY_TYPE) { PuzzleComposerBlockRenderer() }
        }
    }

    private val puzzlePanelRenderer = PuzzlePanelRenderer

    override fun createRenderState(): PuzzleComposerRenderState = PuzzleComposerRenderState()

    override fun updateRenderState(
        blockEntity: PuzzleComposerBlockEntity,
        state: PuzzleComposerRenderState,
        tickDelta: Float,
        cameraPos: Vec3d,
        crumblingOverlay: ModelCommandRenderer.CrumblingOverlayCommand?
    ) {
        BlockEntityRenderState.updateBlockEntityRenderState(blockEntity, state, crumblingOverlay)
        val itemStack: ItemStack = blockEntity.inventory.items[PUZZLE_OUTPUT_SLOT_INDEX]
        state.panel = if (itemStack.isEmpty) null else itemStack.panel ?: Panel.DEFAULT
        state.facing = blockEntity.cachedState.get(Properties.HORIZONTAL_FACING)
        // Get light above
        // TODO: Figure out lighting so that panel is lit properly
        state.lightAbove = blockEntity.world
            ?.let { world -> WorldRenderer.getLightmapCoordinates(world, blockEntity.pos.up()) }
            ?: state.lightmapCoordinates
    }

    override fun render(
        state: PuzzleComposerRenderState,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        cameraState: CameraRenderState
    ) {
        val panel: Panel = state.panel ?: return
        matrices.push()

        // Move to center
        matrices.translate(.5, .815, .5)

        // Rotate the entity to the direction of the block
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.facing.positiveHorizontalDegrees))

        // Rotate to horizontal plane
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f))

        // Scale the panel
        matrices.scale(0.85f, 0.85f, 1f)

        // Move slightly out of center to avoid z collision
        matrices.translate(-.475, -.475, -.125)

        // Scale to fit to frame
        matrices.scale(0.95f, 0.95f, 1f)

        // Render puzzle panel
        puzzlePanelRenderer.renderPanel(panel, matrices, queue, state.lightAbove, OverlayTexture.DEFAULT_UV)

        matrices.pop()
    }
}
