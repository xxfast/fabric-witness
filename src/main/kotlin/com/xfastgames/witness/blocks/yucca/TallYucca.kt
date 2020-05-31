package com.xfastgames.witness.blocks.yucca

import net.minecraft.block.BlockState
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import java.util.*

class TallYucca : Yucca() {

    companion object {
        val BLOCK by lazy { TallYucca() }
    }

    override fun grow(world: ServerWorld, random: Random?, pos: BlockPos, state: BlockState) {
        dropStack(world, pos, ItemStack(Yucca.BLOCK))
    }
}