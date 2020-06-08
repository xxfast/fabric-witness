package com.xfastgames.witness.blocks.stained.stone.bricks

import com.xfastgames.witness.WITNESS_ID
import com.xfastgames.witness.blocks.stained.stone.stainedStoneSettings
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.Block
import net.minecraft.util.Identifier

class StainedStoneBricks : Block(stainedStoneSettings) {
    companion object {
        val IDENTIFIER = Identifier(WITNESS_ID, "yellow_stained_stone_bricks")
        val BLOCK = registerBlock(StainedStoneBricks(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }
}