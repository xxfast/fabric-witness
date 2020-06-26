package com.xfastgames.witness.blocks.stained.stone.bricks

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.stained.stone.stainedStoneSettings
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.SlabBlock
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier

class StainedStoneBricksSlabs : SlabBlock(stainedStoneSettings) {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "yellow_stained_stone_bricks_slabs")
        val BLOCK = registerBlock(StainedStoneBricksSlabs(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.BUILDING_BLOCKS))
    }

}