package com.xfastgames.witness.utils

import net.minecraft.util.math.BlockPos

val BlockPos.neighbours
    get() = listOf(north(), east(), south(), west(), up(), down())