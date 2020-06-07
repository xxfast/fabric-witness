package com.xfastgames.witness.blocks.leaves

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.block.Material
import net.minecraft.block.VineBlock
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.color.item.ItemColorProvider
import net.minecraft.sound.BlockSoundGroup

class OakLeavesRunners : VineBlock(
    FabricBlockSettings
        .of(Material.REPLACEABLE_PLANT)
        .noCollision()
        .ticksRandomly()
        .strength(0.2f)
        .sounds(BlockSoundGroup.GRASS)
) {

    companion object {
        val BLOCK by lazy { OakLeavesRunners() }
    }

    init {
        ColorProviderRegistry.BLOCK.register(BlockColorProvider { _, _, _, _ -> 0xA0AB42 }, this)
        ColorProviderRegistry.ITEM.register(ItemColorProvider { _, _ -> 0xA0AB42 }, this)
    }
}