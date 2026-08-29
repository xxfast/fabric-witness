package com.xfastgames.witness.entities.renderer

import com.xfastgames.witness.blocks.redstone.IronPuzzleFrameBlock
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.panel
import com.xfastgames.witness.items.renderer.PuzzlePanelRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.level.CameraRenderState
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import com.mojang.math.Axis
import net.minecraft.world.phys.Vec3

class PuzzleFrameRenderState : BlockEntityRenderState() {
    var panel: Panel? = null
    var facing: Direction = Direction.NORTH
    /** Off frames draw the panel dark (rules/minecraft/05-puzzle-frame.md). */
    var powered: Boolean = false
    /**
     * Frozen snapshot of the frame's block pos at extract time. Attract / error flashes key off
     * this so a mutable [pos] reference can never drift between trigger and draw.
     */
    var framePos: BlockPos = BlockPos.ZERO
}

class PuzzleFrameBlockRenderer : BlockEntityRenderer<PuzzleFrameBlockEntity, PuzzleFrameRenderState> {

    companion object {
        const val PUZZLE_FRAME_SCALE = 0.85f

        fun register() {
            BlockEntityRenderers.register(PuzzleFrameBlockEntity.ENTITY_TYPE) { PuzzleFrameBlockRenderer() }
        }
    }

    private val puzzlePanelRenderer = PuzzlePanelRenderer

    override fun createRenderState(): PuzzleFrameRenderState = PuzzleFrameRenderState()

    override fun extractRenderState(
        blockEntity: PuzzleFrameBlockEntity,
        state: PuzzleFrameRenderState,
        tickDelta: Float,
        cameraPos: Vec3,
        crumblingOverlay: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(blockEntity, state, crumblingOverlay)
        val itemStack: ItemStack = blockEntity.inventory.items[0]
        state.panel = if (itemStack.isEmpty) null else itemStack.panel ?: Panel.DEFAULT
        state.facing = blockEntity.blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
        state.powered = blockEntity.blockState.getValue(IronPuzzleFrameBlock.POWERED)
        // Snapshot now: do not hand the live BE pos reference into flash matching.
        state.framePos = blockEntity.blockPos.immutable()
    }

    override fun submit(
        state: PuzzleFrameRenderState,
        matrices: PoseStack,
        queue: SubmitNodeCollector,
        cameraState: CameraRenderState
    ) {
        val panel: Panel = state.panel ?: return
        matrices.pushPose()

        // Move to center
        matrices.translate(.5, .5, .5)

        // Rot the entity to the direction of the block
        matrices.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()))

        // Scale the panel
        matrices.scale(PUZZLE_FRAME_SCALE, PUZZLE_FRAME_SCALE, 1f)

        // Move slightly out of center to avoid z collision
        matrices.translate(.0, .0, -.034)

        // Move to corner
        matrices.translate(-.5, -.5, -.05)

        // Pass frame pos so attract / error flashes only hit this panel, not neighbours.
        puzzlePanelRenderer.renderPanel(
            panel, matrices, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, state.framePos,
            lit = state.powered,
        )
        matrices.popPose()
    }
}
