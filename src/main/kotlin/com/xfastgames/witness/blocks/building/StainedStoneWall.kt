package com.xfastgames.witness.blocks.building

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.WallBlock
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier

class StainedStoneWall : WallBlock(stainedStoneSettings) {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "yellow_stained_stone_walls")
        val BLOCK = registerBlock(StainedStoneWall(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.BUILDING_BLOCKS))
    }

}
