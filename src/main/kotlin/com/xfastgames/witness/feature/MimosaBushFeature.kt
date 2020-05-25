package com.xfastgames.witness.feature

import com.xfastgames.witness.blocks.flowers.MimosaBush
import net.minecraft.world.biome.Biome
import net.minecraft.world.biome.Biomes

class MimosaBushFeature : PatchOfBlocksFeature(MimosaBush.BLOCK) {

    override val biomes: List<Biome> = listOf(
        Biomes.SWAMP,
        Biomes.SWAMP_HILLS
    )
}