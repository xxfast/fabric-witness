package com.xfastgames.witness.feature

import com.xfastgames.witness.blocks.flowers.LilacBush
import net.minecraft.world.biome.Biome
import net.minecraft.world.biome.Biomes

class LilacBushFeature : PatchOfBlocksFeature(LilacBush) {

    override val biomes: List<Biome> = listOf(
        Biomes.SWAMP,
        Biomes.SWAMP_HILLS
    )
}