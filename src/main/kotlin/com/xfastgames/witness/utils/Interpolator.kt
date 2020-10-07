package com.xfastgames.witness.utils

data class Interpolator<T : Comparable<T>>(
    val start: T,
    val end: T,
    val interpolator: (Interpolator<T>) -> Unit
) {

    var value: T = start
        set(value) {
            field = when {
                start < end -> value.coerceAtLeast(start).coerceAtMost(end)
                start > end -> value.coerceAtMost(start).coerceAtLeast(end)
                else -> start
            }
        }

    fun interpolate() {
        interpolator(this)
    }
}

