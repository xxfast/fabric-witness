package com.xfastgames.witness.blocks.stained.stone.bricks

import com.xfastgames.witness.blocks.stained.stone.stainedStoneSettings
import net.minecraft.block.Block

class StainedStoneBricks : Block(stainedStoneSettings) {
    companion object {
        val BLOCK by lazy { StainedStoneBricks() }
    }
}