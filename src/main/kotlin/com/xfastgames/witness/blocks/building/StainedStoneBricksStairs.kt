package com.xfastgames.witness.blocks.building

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.StairsBlock
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier

class StainedStoneBricksStairs(state: BlockState) : StairsBlock(state, stainedStoneSettings) {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "yellow_stained_stone_bricks_stairs")
        val BLOCK = registerBlock(StainedStoneBricksStairs(Blocks.BRICK_STAIRS.defaultState), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.BUILDING_BLOCKS))
    }

}