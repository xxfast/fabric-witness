package com.xfastgames.witness.utils

import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.RotationAxis
import org.joml.Vector3f

fun MatrixStack.rotate(axis: Vector3f, angleDegrees: Float) =
    this.multiply(RotationAxis.of(axis).rotationDegrees(angleDegrees))
