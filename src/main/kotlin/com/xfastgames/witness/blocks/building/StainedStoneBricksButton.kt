package com.xfastgames.witness.blocks.building

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.properties.BlockSetType
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.resources.Identifier

class StainedStoneBricksButton(settings: BlockBehaviour.Properties) :
    ButtonBlock(BlockSetType.STONE, 20, settings) {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "yellow_stained_stone_bricks_button")
        val BLOCK = registerBlock(StainedStoneBricksButton(stainedStoneSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }
}
