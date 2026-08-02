package com.xfastgames.witness.entities.renderer

import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.panel
import com.xfastgames.witness.items.renderer.PuzzlePanelRenderer
import com.xfastgames.witness.screens.composer.PuzzleComposerScreen.Companion.PUZZLE_OUTPUT_SLOT_INDEX
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Direction
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3

class PuzzleComposerRenderState : BlockEntityRenderState() {
    var panel: Panel? = null
    var facing: Direction = Direction.NORTH
    var lightAbove: Int = 0
}

class PuzzleComposerBlockRenderer : BlockEntityRenderer<PuzzleComposerBlockEntity, PuzzleComposerRenderState> {

    companion object {
        fun register() {
            BlockEntityRenderers.register(PuzzleComposerBlockEntity.ENTITY_TYPE) { PuzzleComposerBlockRenderer() }
        }
    }

    private val puzzlePanelRenderer = PuzzlePanelRenderer

    override fun createRenderState(): PuzzleComposerRenderState = PuzzleComposerRenderState()

    override fun extractRenderState(
        blockEntity: PuzzleComposerBlockEntity,
        state: PuzzleComposerRenderState,
        tickDelta: Float,
        cameraPos: Vec3,
        crumblingOverlay: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(blockEntity, state, crumblingOverlay)
        val itemStack: ItemStack = blockEntity.inventory.items[PUZZLE_OUTPUT_SLOT_INDEX]
        state.panel = if (itemStack.isEmpty) null else itemStack.panel ?: Panel.DEFAULT
        state.facing = blockEntity.blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
        // Light from the block above the composer surface.
        // TODO: Figure out lighting so that panel is lit properly
        state.lightAbove = blockEntity.level
            ?.let { level -> LightCoordsUtil.getLightCoords(level, blockEntity.blockPos.above()) }
            ?: state.lightCoords
    }

    override fun submit(
        state: PuzzleComposerRenderState,
        matrices: PoseStack,
        queue: SubmitNodeCollector,
        cameraState: CameraRenderState
    ) {
        val panel: Panel = state.panel ?: return
        matrices.pushPose()

        // Move to center
        matrices.translate(.5, .815, .5)

        // Rot the entity to the direction of the block
        matrices.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()))

        // Rot to horizontal plane
        matrices.mulPose(Axis.XP.rotationDegrees(90.0f))

        // Scale the panel
        matrices.scale(0.85f, 0.85f, 1f)

        // Move slightly out of center to avoid z collision
        matrices.translate(-.475, -.475, -.125)

        // Scale to fit to frame
        matrices.scale(0.95f, 0.95f, 1f)

        // Render puzzle panel
        puzzlePanelRenderer.renderPanel(panel, matrices, queue, state.lightAbove, OverlayTexture.NO_OVERLAY)

        matrices.popPose()
    }
}
