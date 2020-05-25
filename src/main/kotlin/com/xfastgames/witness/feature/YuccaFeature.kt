package com.xfastgames.witness.feature

import com.xfastgames.witness.blocks.yucca.Yucca
import net.minecraft.world.biome.Biome
import net.minecraft.world.biome.Biomes

class YuccaFeature : PatchOfBlocksFeature(Yucca.BLOCK, 25..50) {
    override val biomes: List<Biome> = listOf(
        Biomes.SAVANNA,
        Biomes.SAVANNA_PLATEAU,
        Biomes.SHATTERED_SAVANNA,
        Biomes.SHATTERED_SAVANNA_PLATEAU
    )
}