package com.xfastgames.witness

import com.xfastgames.witness.blocks.building.*
import com.xfastgames.witness.blocks.decorations.*
import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.blocks.redstone.PuzzleFrameBlock
import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.features.JasmineBushFeature
import com.xfastgames.witness.features.MimosaBushFeature
import com.xfastgames.witness.features.PinkCedarTreeFeature
import com.xfastgames.witness.features.YuccaFeature
import com.xfastgames.witness.items.AncientPuzzleTablet
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.screens.PuzzleScreen
import com.xfastgames.witness.utils.BiomeFeature
import com.xfastgames.witness.utils.Clientside
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.api.ModInitializer
import net.minecraft.block.Block
import net.minecraft.item.Item

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
            CedarLog.BLOCK,
            PuzzleFrameBlock.BLOCK,
            PuzzleComposerBlock.BLOCK
        )

        val ITEMS: List<Item> = listOf(
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
            CedarLog.BLOCK_ITEM,
            PuzzleFrameBlock.BLOCK_ITEM,
            PuzzleComposerBlock.BLOCK_ITEM,
            PuzzlePanelItem.ITEM,
            AncientPuzzleTablet.ITEM
        )

        val FEATURES: List<BiomeFeature<*, *>> = listOf(
            YuccaFeature,
            JasmineBushFeature,
            MimosaBushFeature,
            PinkCedarTreeFeature
        )

        val ENTITIES: List<Clientside> = listOf(
            PuzzleFrameBlockEntity.Companion,
            PuzzleComposerBlockEntity.Companion
        )
    }

    override fun onInitialize() {
        FEATURES.forEach { it.register() }
    }

    @Environment(EnvType.CLIENT)
    override fun onInitializeClient() {
        val SCREENS: List<Clientside> = listOf(PuzzleScreen.Companion)

        listOf(BLOCKS, ITEMS, FEATURES, ENTITIES, SCREENS)
            .flatten()
            .filterIsInstance<Clientside>()
            .forEach { it.onClient() }
    }

}