package com.xfastgames.witness.utils

import net.minecraft.world.biome.Biome
import net.minecraft.world.gen.decorator.ConfiguredDecorator
import net.minecraft.world.gen.decorator.DecoratorConfig
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.FeatureConfig

/* Wait for new biome-apis https://github.com/FabricMC/fabric/pull/982 **/
abstract class BiomeFeature<FC : FeatureConfig, DC : DecoratorConfig> {
    abstract val biomes: List<Biome>
    abstract val feature: Feature<FC>
    abstract val configuration: FC
    abstract val decorator: ConfiguredDecorator<DC>

    fun register() {
        TODO()
    }
}