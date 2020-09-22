package com.xfastgames.witness.utils

data class Interpolator(var min: Double, var max: Double, var speed: Double) {
    val value: Double get() = min

    fun interpolate() {
        if (min < max) min += speed
    }
}

