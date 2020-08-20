package com.xfastgames.witness.features

import com.xfastgames.witness.blocks.decorations.JasmineBush
import com.xfastgames.witness.utils.BiomeFeature
import net.minecraft.block.Blocks
import net.minecraft.world.biome.Biome
import net.minecraft.world.gen.decorator.ChanceDecoratorConfig
import net.minecraft.world.gen.decorator.ConfiguredDecorator
import net.minecraft.world.gen.decorator.Decorator
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.RandomPatchFeatureConfig
import net.minecraft.world.gen.placer.SimpleBlockPlacer
import net.minecraft.world.gen.stateprovider.SimpleBlockStateProvider

object JasmineBushFeature : BiomeFeature<RandomPatchFeatureConfig, ChanceDecoratorConfig>() {

    override val biomes: List<Biome> = emptyList()

    override val feature: Feature<RandomPatchFeatureConfig> = Feature.RANDOM_PATCH

    override val configuration: RandomPatchFeatureConfig = RandomPatchFeatureConfig
        .Builder(
            SimpleBlockStateProvider(JasmineBush.BLOCK.defaultState),
            SimpleBlockPlacer()
        ).tries(64)
        .whitelist(mutableSetOf(Blocks.GRASS_BLOCK))
        .cannotProject().build()

    override val decorator: ConfiguredDecorator<ChanceDecoratorConfig> =
        Decorator.CHANCE.configure(ChanceDecoratorConfig(10))
}