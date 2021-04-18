package com.xfastgames.witness.utils

infix fun <T : Comparable<T>> ClosedFloatingPointRange<T>.intersects(other: ClosedFloatingPointRange<T>): Boolean {
    val x1 = this.min
    val x2 = this.max
    val y1 = other.min
    val y2 = other.max
    return x1 <= y2 && y1 <= x2
}

infix fun ClosedFloatingPointRange<Float>.intersection(other: ClosedFloatingPointRange<Float>): ClosedFloatingPointRange<Float>? {
    if (!(this intersects other)) return null
    val min: Float = if (other.min > this.min) other.min else this.min
    val max: Float = if (other.max > this.max) this.max else other.max
    return min..max
}

val <T : Comparable<T>> ClosedFloatingPointRange<T>.min: T
    get() =
        if (start < endInclusive) start
        else endInclusive

val <T : Comparable<T>> ClosedFloatingPointRange<T>.max: T
    get() =
        if (start > endInclusive) start
        else endInclusive

val ClosedFloatingPointRange<Float>.average: Float
    get() = start + endInclusive / 2

val ClosedFloatingPointRange<Float>.mid: Float
    get() = min + (max - min) / 2

