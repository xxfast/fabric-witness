package com.xfastgames.witness.blocks.logs

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.LogBlock
import net.minecraft.block.Material
import net.minecraft.block.MaterialColor
import net.minecraft.sound.BlockSoundGroup

class CedarLog : LogBlock(
    MaterialColor.WOOD,
    FabricBlockSettings.of(Material.WOOD, MaterialColor.SPRUCE).strength(2.0f)
        .sounds(BlockSoundGroup.WOOD)
) {

    companion object {
        val BLOCK by lazy { CedarLog() }
    }
}