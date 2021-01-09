package com.xfastgames.witness.utils

infix fun <T : Comparable<T>> ClosedFloatingPointRange<T>.intersects(other: ClosedFloatingPointRange<T>): Boolean {
    val x1 = this.min
    val x2 = this.max
    val y1 = other.min
    val y2 = other.max
    return x1 <= y2 && y1 <= x2
}

val <T : Comparable<T>> ClosedFloatingPointRange<T>.min: T
    get() =
        if (start < endInclusive) start
        else endInclusive


val <T : Comparable<T>> ClosedFloatingPointRange<T>.max: T
    get() =
        if (start > endInclusive) start
        else endInclusive