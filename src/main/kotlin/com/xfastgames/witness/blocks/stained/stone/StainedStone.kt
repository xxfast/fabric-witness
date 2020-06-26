package com.xfastgames.witness.blocks.stained.stone

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.stained.stone.bricks.StainedStoneBricks
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier

class StainedStone : Block(stainedStoneSettings) {
    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "yellow_stained_stone")
        val BLOCK = registerBlock(StainedStoneBricks(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.BUILDING_BLOCKS))
    }
}