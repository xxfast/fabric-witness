package com.xfastgames.witness.feature

import com.xfastgames.witness.blocks.flowers.JasmineBush
import net.minecraft.world.biome.Biome
import net.minecraft.world.biome.Biomes

class JasmineBushFeature : PatchOfBlocksFeature(JasmineBush) {

    override val biomes: List<Biome> = listOf(
        Biomes.SWAMP,
        Biomes.SWAMP_HILLS
    )
}