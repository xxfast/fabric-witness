package com.xfastgames.witness.entities.renderer

import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.panel
import com.xfastgames.witness.items.renderer.PuzzlePanelRenderer
import net.minecraft.client.render.OverlayTexture
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

class PuzzleFrameRenderState : BlockEntityRenderState() {
    var panel: Panel? = null
    var facing: Direction = Direction.NORTH
}

class PuzzleFrameBlockRenderer : BlockEntityRenderer<PuzzleFrameBlockEntity, PuzzleFrameRenderState> {

    companion object {
        const val PUZZLE_FRAME_SCALE = 0.85f

        fun register() {
            BlockEntityRendererFactories.register(PuzzleFrameBlockEntity.ENTITY_TYPE) { PuzzleFrameBlockRenderer() }
        }
    }

    private val puzzlePanelRenderer = PuzzlePanelRenderer

    override fun createRenderState(): PuzzleFrameRenderState = PuzzleFrameRenderState()

    override fun updateRenderState(
        blockEntity: PuzzleFrameBlockEntity,
        state: PuzzleFrameRenderState,
        tickDelta: Float,
        cameraPos: Vec3d,
        crumblingOverlay: ModelCommandRenderer.CrumblingOverlayCommand?
    ) {
        BlockEntityRenderState.updateBlockEntityRenderState(blockEntity, state, crumblingOverlay)
        val itemStack: ItemStack = blockEntity.inventory.items[0]
        state.panel = if (itemStack.isEmpty) null else itemStack.panel ?: Panel.DEFAULT
        state.facing = blockEntity.cachedState.get(Properties.HORIZONTAL_FACING)
    }

    override fun render(
        state: PuzzleFrameRenderState,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        cameraState: CameraRenderState
    ) {
        val panel: Panel = state.panel ?: return
        matrices.push()

        // Move to center
        matrices.translate(.5, .5, .5)

        // Rotate the entity to the direction of the block
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.facing.positiveHorizontalDegrees))

        // Scale the panel
        matrices.scale(PUZZLE_FRAME_SCALE, PUZZLE_FRAME_SCALE, 1f)

        // Move slightly out of center to avoid z collision
        matrices.translate(.0, .0, -.034)

        // Move to corner
        matrices.translate(-.5, -.5, -.05)

        // Render puzzle panel
        puzzlePanelRenderer.renderPanel(panel, matrices, queue, state.lightmapCoordinates, OverlayTexture.DEFAULT_UV)
        matrices.pop()
    }
}
