package com.xfastgames.witness.blocks.building

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.AbstractBlock
import net.minecraft.block.SlabBlock
import net.minecraft.util.Identifier

class StainedStoneBricksSlabs(settings: AbstractBlock.Settings) : SlabBlock(settings) {

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "yellow_stained_stone_bricks_slabs")
        val BLOCK = registerBlock(StainedStoneBricksSlabs(stainedStoneSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

}
