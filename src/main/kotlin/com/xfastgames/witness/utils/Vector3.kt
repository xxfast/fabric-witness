package com.xfastgames.witness.utils

import net.minecraft.client.util.math.Vector3d
import net.minecraft.util.math.Quaternion
import net.minecraft.util.math.Vec3f

fun Vector3d.rotate(axis: Vec3f, angleDegrees: Float): Vector3d {
    val rotation: Quaternion = axis.getDegreesQuaternion(angleDegrees)
    val quaternion = Quaternion(rotation)
    quaternion.hamiltonProduct(Quaternion(this.x.toFloat(), this.y.toFloat(), this.z.toFloat(), 0.0f))
    val quaternion2 = Quaternion(rotation)
    quaternion2.conjugate()
    quaternion.hamiltonProduct(quaternion2)
    return Vector3d(quaternion.x.toDouble(), quaternion.y.toDouble(), quaternion.z.toDouble())
}

fun Vec3f.rotate(axis: Vec3f, angleDegrees: Float) =
    this.rotate(axis.getDegreesQuaternion(angleDegrees))

operator fun Vec3f.minus(other: Vec3f): Vec3f = this.copy().apply { subtract(other) }
operator fun Vec3f.plus(other: Vec3f): Vec3f = this.copy().apply { add(other) }
operator fun Vec3f.div(other: Vec3f): Vec3f =
    this.copy().apply { multiplyComponentwise(1f / other.x, 1f / other.y, 1f / other.z) }

operator fun Vec3f.div(other: Float): Vec3f =
    this.copy().apply { multiplyComponentwise(1f / other, 1f / other, 1f / other) }

fun maxOf(first: Vec3f, second: Vec3f): Vec3f =
    if (first.x > second.x && first.y > second.y) first
    else second

fun minOf(first: Vec3f, second: Vec3f): Vec3f =
    if (first.x < second.x && first.y < second.y) first
    else second