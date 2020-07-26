package com.xfastgames.witness.features

import com.xfastgames.witness.blocks.decorations.TallYucca
import com.xfastgames.witness.blocks.decorations.Yucca
import com.xfastgames.witness.utils.BiomeFeature
import net.minecraft.block.Blocks
import net.minecraft.world.biome.Biome
import net.minecraft.world.biome.Biomes
import net.minecraft.world.gen.decorator.ChanceDecoratorConfig
import net.minecraft.world.gen.decorator.ConfiguredDecorator
import net.minecraft.world.gen.decorator.Decorator
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.RandomPatchFeatureConfig
import net.minecraft.world.gen.placer.SimpleBlockPlacer
import net.minecraft.world.gen.stateprovider.WeightedBlockStateProvider

object YuccaFeature : BiomeFeature<RandomPatchFeatureConfig, ChanceDecoratorConfig>() {

    override val biomes: List<Biome> = listOf(
        Biomes.SAVANNA,
        Biomes.SAVANNA_PLATEAU,
        Biomes.SHATTERED_SAVANNA,
        Biomes.SHATTERED_SAVANNA_PLATEAU
    )

    override val feature: Feature<RandomPatchFeatureConfig> = Feature.RANDOM_PATCH

    override val configuration: RandomPatchFeatureConfig = RandomPatchFeatureConfig
        .Builder(
            WeightedBlockStateProvider()
                .addState(Yucca.BLOCK.defaultState, 2)
                .addState(TallYucca.BLOCK.defaultState, 1),
            SimpleBlockPlacer()
        )
        .tries(64)
        .whitelist(mutableSetOf(Blocks.GRASS_BLOCK))
        .cannotProject()
        .build()

    override val decorator: ConfiguredDecorator<ChanceDecoratorConfig> =
        Decorator.CHANCE_HEIGHTMAP_DOUBLE.configure(ChanceDecoratorConfig(10))
}
