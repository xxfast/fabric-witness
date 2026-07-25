package com.xfastgames.witness.items.renderer

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.panel
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.item.model.special.SpecialModelRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemDisplayContext
import net.minecraft.item.ItemStack
import org.joml.Vector3f
import org.joml.Vector3fc
import java.util.function.Consumer

/**
 * Item-model bridge for the live puzzle panel.
 *
 * The 1.21.4 item-model rewrite removed Fabric's old dynamic item renderer. A special item model is
 * the replacement: [getData] snapshots the stack component into the item render state and [render]
 * submits the same panel geometry used by frames and the composer.
 */
object PuzzlePanelSpecialModelRenderer : SpecialModelRenderer<Panel> {

    override fun getData(stack: ItemStack): Panel = stack.panel ?: Panel.DEFAULT

    override fun render(
        panel: Panel?,
        displayContext: ItemDisplayContext,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int,
        glint: Boolean,
        outlineColor: Int
    ) {
        matrices.push()

        // Generated-item transforms expect geometry in the unit cube. Keep the live panel on its
        // centre plane, with the same orientation as an ordinary flat item model.
        matrices.translate(1.0, 0.0, 0.5)
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180.0f))
        PuzzlePanelRenderer.renderPanel(panel ?: Panel.DEFAULT, matrices, queue, light, overlay)

        matrices.pop()
    }

    override fun collectVertices(consumer: Consumer<Vector3fc>) {
        consumer.accept(Vector3f(0.0f, 0.0f, 0.5f))
        consumer.accept(Vector3f(1.0f, 0.0f, 0.5f))
        consumer.accept(Vector3f(1.0f, 1.0f, 0.5f))
        consumer.accept(Vector3f(0.0f, 1.0f, 0.5f))
    }

    object Unbaked : SpecialModelRenderer.Unbaked {
        private val codec: MapCodec<Unbaked> = MapCodec.unit(this)

        override fun bake(context: SpecialModelRenderer.BakeContext): SpecialModelRenderer<*> =
            PuzzlePanelSpecialModelRenderer

        override fun getCodec(): MapCodec<out SpecialModelRenderer.Unbaked> = codec
    }
}
