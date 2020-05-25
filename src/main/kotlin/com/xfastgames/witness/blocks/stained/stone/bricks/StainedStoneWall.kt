package com.xfastgames.witness.blocks.stained.stone.bricks

import com.xfastgames.witness.blocks.stained.stone.stainedStoneSettings
import net.minecraft.block.WallBlock

class StainedStoneWall : WallBlock(stainedStoneSettings) {

    companion object {
        val BLOCK by lazy { StainedStoneWall() }
    }

}
