package com.xfastgames.witness.utils

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import org.joml.Vector3f

fun PoseStack.rotate(axis: Vector3f, angleDegrees: Float) =
    this.mulPose(Axis.of(axis).rotationDegrees(angleDegrees))
