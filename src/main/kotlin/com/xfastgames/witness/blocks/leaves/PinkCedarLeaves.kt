package com.xfastgames.witness.blocks.leaves

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Blocks
import net.minecraft.block.LeavesBlock
import net.minecraft.block.Material
import net.minecraft.sound.BlockSoundGroup

class PinkCedarLeaves : LeavesBlock(
    FabricBlockSettings.of(Material.LEAVES)
        .strength(0.2F)
        .ticksRandomly()
        .sounds(BlockSoundGroup.GRASS)
        .nonOpaque()
) {
    companion object {
        val x = Blocks.OAK_LOG
        val BLOCK by lazy { PinkCedarLeaves() }
    }
}