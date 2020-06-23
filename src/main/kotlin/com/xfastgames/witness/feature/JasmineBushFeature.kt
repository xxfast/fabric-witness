package com.xfastgames.witness.feature

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.flowers.JasmineBush
import com.xfastgames.witness.utils.registerFeature
import net.minecraft.util.Identifier
import net.minecraft.world.biome.Biomes
import net.minecraft.world.gen.GenerationStep
import net.minecraft.world.gen.decorator.ChanceDecoratorConfig
import net.minecraft.world.gen.decorator.Decorator
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.FeatureConfig

class JasmineBushFeature : PatchOfBlocksFeature(listOf(JasmineBush.BLOCK)) {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "jasmine_bush_growth")

        val FEATURE: Feature<FeatureConfig> = registerFeature(
            id = IDENTIFIER,
            feature = JasmineBushFeature(),
            biomes = listOf(
                Biomes.TAIGA,
                Biomes.TAIGA_MOUNTAINS,
                Biomes.TAIGA_HILLS
            )
        ) { registeredFeature, biome ->
            biome.addFeature(
                GenerationStep.Feature.VEGETAL_DECORATION, registeredFeature
                    .configure(FeatureConfig.DEFAULT)
                    .createDecoratedFeature(Decorator.CHANCE_HEIGHTMAP.configure(ChanceDecoratorConfig(100)))
            )
        }
    }

}