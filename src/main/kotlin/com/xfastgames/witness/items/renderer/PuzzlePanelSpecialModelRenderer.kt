package com.xfastgames.witness.items.renderer

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.panel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.special.SpecialModelRenderer
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.item.ItemStack
import org.joml.Vector3f
import org.joml.Vector3fc
import java.util.function.Consumer

/**
 * Item-model bridge for the live puzzle panel.
 *
 * The 1.21.4 item-model rewrite removed Fabric's old dynamic item renderer. A special item model is
 * the replacement: [extractArgument] snapshots the stack component into the item render state and
 * [submit] submits the same panel geometry used by frames and the composer.
 */
object PuzzlePanelSpecialModelRenderer : SpecialModelRenderer<Panel> {

    override fun extractArgument(stack: ItemStack): Panel = stack.panel ?: Panel.DEFAULT

    override fun submit(
        panel: Panel?,
        matrices: PoseStack,
        queue: SubmitNodeCollector,
        light: Int,
        overlay: Int,
        glint: Boolean,
        outlineColor: Int
    ) {
        matrices.pushPose()

        // Generated-item transforms expect geometry in the unit cube. Keep the live panel on its
        // centre plane, with the same orientation as an ordinary flat item model.
        matrices.translate(1.0, 0.0, 0.5)
        matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f))
        PuzzlePanelRenderer.renderPanel(panel ?: Panel.DEFAULT, matrices, queue, light, overlay)

        matrices.popPose()
    }

    override fun getExtents(consumer: Consumer<Vector3fc>) {
        consumer.accept(Vector3f(0.0f, 0.0f, 0.5f))
        consumer.accept(Vector3f(1.0f, 0.0f, 0.5f))
        consumer.accept(Vector3f(1.0f, 1.0f, 0.5f))
        consumer.accept(Vector3f(0.0f, 1.0f, 0.5f))
    }

    object Unbaked : SpecialModelRenderer.Unbaked<Panel> {
        private val codec: MapCodec<Unbaked> = MapCodec.unit(this)

        override fun bake(context: SpecialModelRenderer.BakingContext): SpecialModelRenderer<Panel> =
            PuzzlePanelSpecialModelRenderer

        override fun type(): MapCodec<out SpecialModelRenderer.Unbaked<Panel>> = codec
    }
}
