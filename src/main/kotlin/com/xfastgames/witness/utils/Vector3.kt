package com.xfastgames.witness.utils

import net.minecraft.client.util.math.Vector3d
import net.minecraft.client.util.math.Vector3f
import net.minecraft.util.math.Quaternion

fun Vector3d.rotate(axis: Vector3f, angleDegrees: Float): Vector3d {
    val rotation: Quaternion = axis.getDegreesQuaternion(angleDegrees)
    val quaternion = Quaternion(rotation)
    quaternion.hamiltonProduct(Quaternion(this.x.toFloat(), this.y.toFloat(), this.z.toFloat(), 0.0f))
    val quaternion2 = Quaternion(rotation)
    quaternion2.conjugate()
    quaternion.hamiltonProduct(quaternion2)
    return Vector3d(quaternion.x.toDouble(), quaternion.y.toDouble(), quaternion.z.toDouble())
}

fun Vector3f.rotate(axis: Vector3f, angleDegrees: Float) =
    this.rotate(axis.getDegreesQuaternion(angleDegrees))
