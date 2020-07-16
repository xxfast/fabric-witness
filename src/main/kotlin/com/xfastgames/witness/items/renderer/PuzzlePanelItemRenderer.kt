package com.xfastgames.witness.items.renderer

import com.xfastgames.witness.Witness
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

    override fun render(
        stack: ItemStack?,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val backdropConsumer: VertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(backdropTexture))
        val position = Vector3f(0f, 0f, 0.5f)
        backdropConsumer.square(matrices, position, 1f, light, overlay)
    }
}