package com.xfastgames.witness.utils

import net.minecraft.util.math.BlockPos

val BlockPos.neighbours: List<BlockPos>
    get() = listOf(north(), east(), south(), west(), up(), down())

val BlockPos.above: BlockPos
    get() = this.up()

val BlockPos.below: BlockPos
    get() = this.down()