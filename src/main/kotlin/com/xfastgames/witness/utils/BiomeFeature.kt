package com.xfastgames.witness.utils

import net.minecraft.world.biome.Biome
import net.minecraft.world.gen.GenerationStep
import net.minecraft.world.gen.decorator.ConfiguredDecorator
import net.minecraft.world.gen.decorator.DecoratorConfig
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.FeatureConfig

abstract class BiomeFeature<FC : FeatureConfig, DC : DecoratorConfig> {
    abstract val biomes: List<Biome>
    abstract val feature: Feature<FC>
    abstract val configuration: FC
    abstract val decorator: ConfiguredDecorator<DC>

    fun register() {
        Biome.BIOMES
            .filter { biome: Biome -> biome in biomes }
            .forEach { biome ->
                biome.addFeature(
                    GenerationStep.Feature.VEGETAL_DECORATION,
                    feature.configure(configuration).createDecoratedFeature(decorator)
                )
            }
    }
}