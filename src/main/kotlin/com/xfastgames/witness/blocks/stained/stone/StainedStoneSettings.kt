package com.xfastgames.witness.blocks.stained.stone

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags
import net.minecraft.block.Material
import net.minecraft.sound.BlockSoundGroup

val stainedStoneSettings: FabricBlockSettings =
        FabricBlockSettings.of(Material.STONE)
                .breakByHand(true)
                .breakByTool(FabricToolTags.PICKAXES)
                .sounds(BlockSoundGroup.STONE)
                .strength(1.5f, 6f)