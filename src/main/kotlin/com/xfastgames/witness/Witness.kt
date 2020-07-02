package com.xfastgames.witness

import com.xfastgames.witness.blocks.drapes.BlueBougainvilleaDrape
import com.xfastgames.witness.blocks.drapes.PurpleBougainvilleaDrape
import com.xfastgames.witness.blocks.flowers.JasmineBush
import com.xfastgames.witness.blocks.flowers.MimosaBush
import com.xfastgames.witness.blocks.leaves.OakLeavesRunners
import com.xfastgames.witness.blocks.leaves.PinkCedarLeaves
import com.xfastgames.witness.blocks.logs.CedarLog
import com.xfastgames.witness.blocks.stained.stone.StainedStone
import com.xfastgames.witness.blocks.stained.stone.StainedStoneSlabs
import com.xfastgames.witness.blocks.stained.stone.StainedStoneStairs
import com.xfastgames.witness.blocks.stained.stone.StainedStoneWall
import com.xfastgames.witness.blocks.stained.stone.bricks.*
import com.xfastgames.witness.blocks.yucca.TallYucca
import com.xfastgames.witness.blocks.yucca.Yucca
import com.xfastgames.witness.features.JasmineBushFeature
import com.xfastgames.witness.features.MimosaBushFeature
import com.xfastgames.witness.features.PinkCedarTreeFeature
import com.xfastgames.witness.features.YuccaFeature
import com.xfastgames.witness.utils.Clientside
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.api.ModInitializer
import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.FeatureConfig

class Witness : ModInitializer, ClientModInitializer {

    companion object {
        const val IDENTIFIER = "witness"

        val BLOCKS: List<Block> = listOf(
            StainedStone.BLOCK,
            StainedStoneStairs.BLOCK,
            StainedStoneSlabs.BLOCK,
            StainedStoneWall.BLOCK,
            StainedStoneBricks.BLOCK,
            StainedStoneBricksSlabs.BLOCK,
            StainedStoneBricksStairs.BLOCK,
            StainedStoneBricksWall.BLOCK,
            StainedStoneBricksButton.BLOCK,
            OakLeavesRunners.BLOCK,
            Yucca.BLOCK,
            TallYucca.BLOCK,
            JasmineBush.BLOCK,
            MimosaBush.BLOCK,
            PurpleBougainvilleaDrape.BLOCK,
            BlueBougainvilleaDrape.BLOCK,
            PinkCedarLeaves.BLOCK,
            CedarLog.BLOCK
        )

        val ITEMS: List<BlockItem> = listOf(
            StainedStone.BLOCK_ITEM,
            StainedStoneStairs.BLOCK_ITEM,
            StainedStoneSlabs.BLOCK_ITEM,
            StainedStoneWall.BLOCK_ITEM,
            StainedStoneBricks.BLOCK_ITEM,
            StainedStoneBricksSlabs.BLOCK_ITEM,
            StainedStoneBricksStairs.BLOCK_ITEM,
            StainedStoneBricksWall.BLOCK_ITEM,
            StainedStoneBricksButton.BLOCK_ITEM,
            OakLeavesRunners.BLOCK_ITEM,
            Yucca.BLOCK_ITEM,
            TallYucca.BLOCK_ITEM,
            JasmineBush.BLOCK_ITEM,
            MimosaBush.BLOCK_ITEM,
            PurpleBougainvilleaDrape.BLOCK_ITEM,
            BlueBougainvilleaDrape.BLOCK_ITEM,
            PinkCedarLeaves.BLOCK_ITEM,
            CedarLog.BLOCK_ITEM
        )

        val FEATURES: List<Feature<out FeatureConfig>> = listOf(
            YuccaFeature.FEATURE,
            JasmineBushFeature.FEATURE,
            MimosaBushFeature.FEATURE,
            PinkCedarTreeFeature.FEATURE
        )
    }

    override fun onInitialize() {}

    @Environment(EnvType.CLIENT)
    override fun onInitializeClient() {
        listOf(BLOCKS, ITEMS, FEATURES)
            .flatten()
            .filterIsInstance<Clientside>()
            .forEach { it.onClient() }
    }
}