package com.xfastgames.witness.feature

import com.xfastgames.witness.blocks.flowers.JasmineBush
import net.minecraft.world.biome.Biome
import net.minecraft.world.biome.Biomes

class JasmineBushFeature : PatchOfBlocksFeature(JasmineBush.BLOCK) {

    override val biomes: List<Biome> = listOf(
        Biomes.TAIGA,
        Biomes.TAIGA_MOUNTAINS,
        Biomes.TAIGA_HILLS
    )
}