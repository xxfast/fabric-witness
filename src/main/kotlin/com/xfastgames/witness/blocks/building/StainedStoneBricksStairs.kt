package com.xfastgames.witness.blocks.building

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.StairBlock
import net.minecraft.resources.Identifier

class StainedStoneBricksStairs(state: BlockState, settings: BlockBehaviour.Properties) : StairBlock(state, settings) {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "yellow_stained_stone_bricks_stairs")
        val BLOCK = registerBlock(StainedStoneBricksStairs(Blocks.BRICK_STAIRS.defaultBlockState(), stainedStoneSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

}
