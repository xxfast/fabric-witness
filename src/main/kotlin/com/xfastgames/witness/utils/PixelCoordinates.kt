package com.xfastgames.witness.utils

val Int.pc: Float get() = (1f / 16f) * this
val Float.pc: Float get() = (1f / 16f) * this
val Float.d: Double get() = this.toDouble()