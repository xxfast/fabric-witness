package com.xfastgames.witness.utils

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.PlayerEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.util.Arm
import net.minecraft.util.math.MathHelper

fun renderArmHoldingItem(
    matrices: MatrixStack,
    vertexConsumers: VertexConsumerProvider,
    light: Int,
    equipProgress: Float,
    swingProgress: Float,
    arm: Arm
) {
    matrices.push()
    matrices.scale(5f, 5f, 5f)
    val armOffset = if (arm == Arm.RIGHT) 1.0f else -1.0f
    matrices.translate((armOffset * 0.125f).toDouble(), -0.125, 0.0)
    matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(armOffset * 10.0f))
    val isRightArm: Boolean = arm != Arm.LEFT
    val f = if (isRightArm) 1.0f else -1.0f
    val g = MathHelper.sqrt(swingProgress)
    val h = -0.3f * MathHelper.sin(g * 3.1415927f)
    val i = 0.4f * MathHelper.sin(g * 6.2831855f)
    val j = -0.4f * MathHelper.sin(swingProgress * 3.1415927f)
    matrices.translate(
        (f * (h + 0.64000005f)).toDouble(), (i + -0.6f + equipProgress * -0.6f).toDouble(),
        (j + -0.71999997f).toDouble()
    )
    matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(f * 45.0f))
    val k = MathHelper.sin(swingProgress * swingProgress * 3.1415927f)
    val l = MathHelper.sin(g * 3.1415927f)
    matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(f * l * 70.0f))
    matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(f * k * -20.0f))
    val client: MinecraftClient = MinecraftClient.getInstance()
    val abstractClientPlayerEntity: ClientPlayerEntity = requireNotNull(client.player)
    client.textureManager.bindTexture(abstractClientPlayerEntity.skinTexture)
    matrices.translate((f * -1.0f).toDouble(), 3.5999999046325684, 3.5)
    matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(f * 120.0f))
    matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(200.0f))
    matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(f * -135.0f))
    matrices.translate((f * 5.6f).toDouble(), 0.0, 0.0)
    val playerEntityRenderer =
        client.entityRenderDispatcher.getRenderer<AbstractClientPlayerEntity>(abstractClientPlayerEntity) as PlayerEntityRenderer
    if (isRightArm) {
        playerEntityRenderer.renderRightArm(matrices, vertexConsumers, light, abstractClientPlayerEntity)
    } else {
        playerEntityRenderer.renderLeftArm(matrices, vertexConsumers, light, abstractClientPlayerEntity)
    }
    matrices.pop()
}