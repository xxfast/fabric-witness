package com.xfastgames.witness

import com.xfastgames.witness.blocks.building.*
import com.xfastgames.witness.blocks.decorations.*
import com.xfastgames.witness.blocks.redstone.IronPuzzleFrameBlock
import com.xfastgames.witness.blocks.redstone.IronStandBlock
import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.AncientPuzzleTablet
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.PanelComponents
import com.xfastgames.witness.recipes.PanelDyeRecipe
import com.xfastgames.witness.screens.composer.PUZZLE_COMPOSER_SCREEN_HANDLER
import com.xfastgames.witness.sounds.WitnessSounds
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.level.block.Block
import net.minecraft.world.item.Item

class Witness : ModInitializer {

    companion object {
        const val IDENTIFIER = "witness"

        private fun creativeTab(path: String): ResourceKey<CreativeModeTab> =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(path))

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
            IronPuzzleFrameBlock.BLOCK,
            PuzzleComposerBlock.BLOCK,
            IronStandBlock.BLOCK
        )

        /** Items shown in the building blocks creative tab (was CreativeModeTab.BUILDING_BLOCKS). */
        val BUILDING_ITEMS: List<Item> = listOf(
            StainedStone.BLOCK_ITEM,
            StainedStoneStairs.BLOCK_ITEM,
            StainedStoneSlabs.BLOCK_ITEM,
            StainedStoneWall.BLOCK_ITEM,
            StainedStoneBricks.BLOCK_ITEM,
            StainedStoneBricksSlabs.BLOCK_ITEM,
            StainedStoneBricksStairs.BLOCK_ITEM,
            StainedStoneBricksWall.BLOCK_ITEM,
            CedarLog.BLOCK_ITEM
        )

        /** Items shown in the natural tab (was CreativeModeTab.DECORATIONS, removed in 1.19.3+). */
        val NATURAL_ITEMS: List<Item> = listOf(
            OakLeavesRunners.BLOCK_ITEM,
            Yucca.BLOCK_ITEM,
            TallYucca.BLOCK_ITEM,
            JasmineBush.BLOCK_ITEM,
            MimosaBush.BLOCK_ITEM,
            PurpleBougainvilleaDrape.BLOCK_ITEM,
            BlueBougainvilleaDrape.BLOCK_ITEM,
            PinkCedarLeaves.BLOCK_ITEM
        )

        /** Items shown in the redstone tab (was CreativeModeTab.REDSTONE). */
        val REDSTONE_ITEMS: List<Item> = listOf(
            StainedStoneBricksButton.BLOCK_ITEM,
            IronPuzzleFrameBlock.BLOCK_ITEM,
            PuzzleComposerBlock.BLOCK_ITEM,
            IronStandBlock.BLOCK_ITEM,
            PuzzlePanelItem.ITEM
        )

        /** Items shown in the ingredients tab (was CreativeModeTab.MATERIALS). */
        val INGREDIENT_ITEMS: List<Item> = listOf(
            AncientPuzzleTablet.ITEM
        )

        val ITEMS: List<Item> = BUILDING_ITEMS + NATURAL_ITEMS + REDSTONE_ITEMS + INGREDIENT_ITEMS

        val ENTITIES: List<Any> = listOf(
            PuzzleFrameBlockEntity.Companion,
            PuzzleComposerBlockEntity.Companion
        )
    }

    override fun onInitialize() {
        // Force registration of classes with registration side effects in companions.
        PanelComponents.init()
        PanelDyeRecipe.init()
        WitnessSounds.init()
        BLOCKS.size
        ITEMS.size
        ENTITIES.size
        // Screen handler must be registered during common init (registries freeze afterwards).
        PUZZLE_COMPOSER_SCREEN_HANDLER

        // Attacking a loaded puzzle frame pops the panel instead of breaking the frame, the way
        // hitting an item frame drops its item. Returning SUCCESS cancels the break on both sides.
        AttackBlockCallback.EVENT.register { player, world, _, pos, _ ->
            IronPuzzleFrameBlock.retrieveOnAttack(player, world, pos)
        }

        // Item groups: CreativeModeTabs.* keys are private in 1.21.11, so build ResourceKeys by path.
        // FabricItemGroupEntries implements CreativeModeTab.Output → accept(ItemLike).
        CreativeModeTabEvents.modifyOutputEvent(creativeTab("building_blocks"))
            .register { entries -> BUILDING_ITEMS.forEach { entries.accept(it) } }
        CreativeModeTabEvents.modifyOutputEvent(creativeTab("natural_blocks"))
            .register { entries -> NATURAL_ITEMS.forEach { entries.accept(it) } }
        CreativeModeTabEvents.modifyOutputEvent(creativeTab("redstone_blocks"))
            .register { entries -> REDSTONE_ITEMS.forEach { entries.accept(it) } }
        CreativeModeTabEvents.modifyOutputEvent(creativeTab("ingredients"))
            .register { entries -> INGREDIENT_ITEMS.forEach { entries.accept(it) } }
    }
}
