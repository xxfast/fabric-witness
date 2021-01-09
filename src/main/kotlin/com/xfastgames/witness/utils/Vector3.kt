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

operator fun Vector3f.minus(other: Vector3f): Vector3f = this.copy().apply { subtract(other) }
operator fun Vector3f.plus(other: Vector3f): Vector3f = this.copy().apply { add(other) }
operator fun Vector3f.div(other: Vector3f): Vector3f =
    this.copy().apply { multiplyComponentwise(1f / other.x, 1f / other.y, 1f / other.z) }

operator fun Vector3f.div(other: Float): Vector3f =
    this.copy().apply { multiplyComponentwise(1f / other, 1f / other, 1f / other) }

fun maxOf(first: Vector3f, second: Vector3f): Vector3f =
    if (first.x > second.x && first.y > second.y) first
    else second

fun minOf(first: Vector3f, second: Vector3f): Vector3f =
    if (first.x < second.x && first.y < second.y) first
    else second