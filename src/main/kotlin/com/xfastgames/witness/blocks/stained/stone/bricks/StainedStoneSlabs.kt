package com.xfastgames.witness.blocks.stained.stone.bricks

import com.xfastgames.witness.blocks.stained.stone.stainedStoneSettings
import net.minecraft.block.SlabBlock

class StainedStoneSlabs : SlabBlock(stainedStoneSettings) {

    companion object {
        val BLOCK by lazy { StainedStoneSlabs() }
    }

}