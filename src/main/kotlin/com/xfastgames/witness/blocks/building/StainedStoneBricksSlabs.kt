package com.xfastgames.witness.blocks.building

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.resources.Identifier

class StainedStoneBricksSlabs(settings: BlockBehaviour.Properties) : SlabBlock(settings) {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "yellow_stained_stone_bricks_slabs")
        val BLOCK = registerBlock(StainedStoneBricksSlabs(stainedStoneSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

}
