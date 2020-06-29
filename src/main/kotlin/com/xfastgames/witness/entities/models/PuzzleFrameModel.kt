package com.xfastgames.witness.entities.models

import com.xfastgames.witness.entities.PuzzleFrameEntity
import net.minecraft.client.model.ModelPart
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.util.math.MatrixStack

class PuzzleFrameModel : EntityModel<PuzzleFrameEntity>() {
    private val base: ModelPart

    init {
        textureHeight = 64
        textureWidth = 64
        base = ModelPart(this, 0, 0)
        base.addCuboid(-6f, -6f, -6f, 12f, 12f, 12f)
    }

    override fun render(
        matrices: MatrixStack,
        vertices: VertexConsumer,
        light: Int,
        overlay: Int,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float
    ) {
        // translate model down
        matrices.translate(0.0, 1.125, 0.0)

        // render cube
        base.render(matrices, vertices, light, overlay)
    }

    override fun setAngles(
        entity: PuzzleFrameEntity?,
        limbAngle: Float,
        limbDistance: Float,
        animationProgress: Float,
        headYaw: Float,
        headPitch: Float
    ) {

    }
}