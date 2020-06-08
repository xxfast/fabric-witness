package com.xfastgames.witness.blocks.leaves

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.LeavesBlock
import net.minecraft.block.Material
import net.minecraft.client.render.RenderLayer
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier

class PinkCedarLeaves : LeavesBlock(
    FabricBlockSettings.of(Material.LEAVES)
        .strength(0.2F)
        .ticksRandomly()
        .sounds(BlockSoundGroup.GRASS)
        .nonOpaque()
) {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "pink_cedar_leaves")
        val BLOCK = registerBlock(PinkCedarLeaves(), IDENTIFIER, RenderLayer.getCutout())
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, RenderLayer.getCutout())
    }

}