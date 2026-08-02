package com.xfastgames.witness.utils

import net.minecraft.core.BlockPos

val BlockPos.neighbours: List<BlockPos>
    get() = listOf(north(), east(), south(), west(), above(), below())
