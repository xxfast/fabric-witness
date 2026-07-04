package com.xfastgames.witness.utils

import org.joml.Vector3f

// Vec3f/Quaternion were replaced by JOML types in 1.19.4+. These helpers now operate on org.joml.Vector3f.

operator fun Vector3f.minus(other: Vector3f): Vector3f = Vector3f(this).sub(other)
operator fun Vector3f.plus(other: Vector3f): Vector3f = Vector3f(this).add(other)
operator fun Vector3f.div(other: Vector3f): Vector3f = Vector3f(this).div(other)
operator fun Vector3f.div(other: Float): Vector3f = Vector3f(this).div(other)

fun maxOf(first: Vector3f, second: Vector3f): Vector3f =
    if (first.x > second.x && first.y > second.y) first
    else second

fun minOf(first: Vector3f, second: Vector3f): Vector3f =
    if (first.x < second.x && first.y < second.y) first
    else second
