package com.xfastgames.witness.items.renderer

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.Panel
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.getPanel
import com.xfastgames.witness.utils.pc
import com.xfastgames.witness.utils.rotate
import com.xfastgames.witness.utils.square
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRenderer
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.client.render.entity.PlayerEntityRenderer
import net.minecraft.client.render.item.HeldItemRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Arm
import net.minecraft.util.Identifier

class PuzzlePanelRenderer(val client: MinecraftClient) : BuiltinItemRenderer, HeldItemRenderer(client) {

    private val backdropTexture = Identifier(Witness.IDENTIFIER, "textures/entity/puzzle_panel_backdrop.png")
    private val tileRenderer: PuzzleTileItemRenderer = PuzzleTileItemRenderer()
    private val renderManager: EntityRenderDispatcher by lazy { client.entityRenderManager }
    private val playerEntityRenderer: PlayerEntityRenderer by lazy {
        renderManager.getRenderer(client.player) as PlayerEntityRenderer
    }

    private fun renderPanel(
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
        puzzle.tiles.forEachIndexed { x, row ->
            val dX: Double = x * (xScale.toDouble() * xCount) - ((xScale * xCount) * xScaledOffset)
            matrices.translate(dX, .0, .0)
            row.forEachIndexed { y, tile ->
                val dY: Double = y * (yScale.toDouble() * yCount) - ((yScale * yCount) * yScaledOffset)
                matrices.translate(.0, dY, .0)
                tileRenderer.render(tile, matrices, vertexConsumers, light, overlay)
                matrices.translate(.0, -dY, .0)
            }
            matrices.translate(-dX, .0, .0)
        }
        matrices.scale(1 + xScale, 1 + yScale, 1f)
        matrices.pop()
    }

    override fun render(
        stack: ItemStack,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        renderPanel(stack, matrices, vertexConsumers, light, overlay)
    }

    override fun renderItem(
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider.Immediate,
        player: ClientPlayerEntity,
        light: Int
    ) {
        renderArm(matrices, vertexConsumers, light, player, Arm.LEFT)
        renderArm(matrices, vertexConsumers, light, player, Arm.RIGHT)

        matrices.scale(10f, 10f, 10f)
        renderPanel(player.mainHandStack, matrices, vertexConsumers, light, 1)
    }

    private fun renderArm(
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        player: ClientPlayerEntity,
        arm: Arm
    ) {
        client.textureManager.bindTexture(player.skinTexture)
        matrices.push()
        val positionOffset: Float = if (arm == Arm.RIGHT) 1.0f else -1.0f
        matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(92.0f))
        matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(45.0f))
        matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(positionOffset * -41.0f))
        matrices.translate((positionOffset * 0.3f).toDouble(), -1.100000023841858, 0.44999998807907104)
        if (arm == Arm.RIGHT) playerEntityRenderer.renderRightArm(matrices, vertexConsumers, light, player)
        else playerEntityRenderer.renderLeftArm(matrices, vertexConsumers, light, player)
        matrices.pop()
    }
}