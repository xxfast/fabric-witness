package com.xfastgames.witness.blocks.leaves

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.block.MaterialColor
import net.minecraft.block.VineBlock
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.color.item.ItemColorProvider

class OakLeavesRunners : VineBlock(FabricBlockSettings.of(Material.PLANT).nonOpaque().materialColor(MaterialColor.FOLIAGE)) {
    init {
        ColorProviderRegistry.BLOCK.register(BlockColorProvider { _, _, _, _ -> 0xA0AB42 }, this)
        ColorProviderRegistry.ITEM.register(ItemColorProvider { _, _ -> 0xA0AB42 }, this)
    }

    override fun hasRandomTicks(state: BlockState?): Boolean = true
}