package com.xfastgames.witness

import com.xfastgames.witness.blocks.leaves.OakLeavesRunners
import com.xfastgames.witness.blocks.stained.stone.bricks.*
import com.xfastgames.witness.utils.registerBlock
import net.fabricmc.api.ModInitializer
import net.minecraft.block.Blocks

internal const val WITNESS_ID = "witness"

class Witness : ModInitializer {
    override fun onInitialize() {
        registerBlock(StainedStoneBricks(), "yellow_stained_stone_bricks")
        registerBlock(StainedStoneStairs(Blocks.BRICK_STAIRS.defaultState), "yellow_stained_stone_bricks_stairs")
        registerBlock(StainedStoneSlabs(), "yellow_stained_stone_bricks_slabs")
        registerBlock(StainedStoneWall(), "yellow_stained_stone_bricks_walls")
        registerBlock(StainedStoneButton(), "yellow_stained_stone_bricks_button")
        registerBlock(OakLeavesRunners(), "oak_leaves_runners", true)
    }
}