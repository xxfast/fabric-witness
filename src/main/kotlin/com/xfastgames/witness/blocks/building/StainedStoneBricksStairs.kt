package com.xfastgames.witness.blocks.building

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.StairsBlock
import net.minecraft.util.Identifier

class StainedStoneBricksStairs(state: BlockState, settings: AbstractBlock.Settings) : StairsBlock(state, settings) {

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "yellow_stained_stone_bricks_stairs")
        val BLOCK = registerBlock(StainedStoneBricksStairs(Blocks.BRICK_STAIRS.defaultState, stainedStoneSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

}
