package com.xfastgames.witness.blocks.leaves

import com.xfastgames.witness.WITNESS_ID
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.block.Material
import net.minecraft.block.VineBlock
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.color.item.ItemColorProvider
import net.minecraft.client.render.RenderLayer
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier

class OakLeavesRunners : VineBlock(
    FabricBlockSettings
        .of(Material.REPLACEABLE_PLANT)
        .noCollision()
        .ticksRandomly()
        .strength(0.2f)
        .sounds(BlockSoundGroup.GRASS)
) {

    companion object {
        val IDENTIFIER = Identifier(WITNESS_ID, "oak_leaves_runners")
        val BLOCK = registerBlock(OakLeavesRunners(), IDENTIFIER, RenderLayer.getTranslucent())
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, RenderLayer.getTranslucent())
    }

    init {
        ColorProviderRegistry.BLOCK.register(BlockColorProvider { _, _, _, _ -> 0xA0AB42 }, this)
        ColorProviderRegistry.ITEM.register(ItemColorProvider { _, _ -> 0xA0AB42 }, this)
    }
}