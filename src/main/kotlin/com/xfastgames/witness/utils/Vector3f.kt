package com.xfastgames.witness.utils

import net.minecraft.client.util.math.Vector3f

fun Vector3f.rotate(axis: Vector3f, angleDegrees: Float) =
    this.rotate(axis.getDegreesQuaternion(angleDegrees))
