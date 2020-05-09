package com.xfastgames.witness

import com.xfastgames.witness.blocks.stained.stone.bricks.StainedStoneBricks
import com.xfastgames.witness.blocks.stained.stone.bricks.StainedStoneSlabs
import com.xfastgames.witness.blocks.stained.stone.bricks.StainedStoneStairs
import com.xfastgames.witness.blocks.stained.stone.bricks.StainedStoneWall
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
    }
}