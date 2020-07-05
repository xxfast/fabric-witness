package com.xfastgames.witness.utils

import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f

fun MatrixStack.rotate(axis: Vector3f, angleDegrees: Float) =
    this.multiply(axis.getDegreesQuaternion(angleDegrees))