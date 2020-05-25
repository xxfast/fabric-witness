package com.xfastgames.witness.blocks.stained.stone.bricks

import com.xfastgames.witness.blocks.stained.stone.stainedStoneSettings
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.StairsBlock

class StainedStoneStairs(state: BlockState) : StairsBlock(state, stainedStoneSettings) {

    companion object {
        val BLOCK by lazy { StainedStoneStairs(Blocks.BRICK_STAIRS.defaultState) }
    }

}