package com.xfastgames.witness.blocks.building

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.AbstractBlock
import net.minecraft.block.WallBlock
import net.minecraft.util.Identifier

class StainedStoneWall(settings: AbstractBlock.Settings) : WallBlock(settings) {

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "yellow_stained_stone_walls")
        val BLOCK = registerBlock(StainedStoneWall(stainedStoneSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

}
