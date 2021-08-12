package com.xfastgames.witness.utils

import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec3f

fun MatrixStack.rotate(axis: Vec3f, angleDegrees: Float) =
    this.multiply(axis.getDegreesQuaternion(angleDegrees))