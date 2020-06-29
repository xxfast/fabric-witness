package com.xfastgames.witness.features

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.yucca.TallYucca
import com.xfastgames.witness.blocks.yucca.Yucca
import com.xfastgames.witness.utils.registerFeature
import net.minecraft.util.Identifier
import net.minecraft.world.biome.Biomes
import net.minecraft.world.gen.GenerationStep
import net.minecraft.world.gen.decorator.ChanceDecoratorConfig
import net.minecraft.world.gen.decorator.Decorator
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.FeatureConfig

class YuccaFeature : PatchOfBlocksFeature(
    blocks = listOf(Yucca.BLOCK, TallYucca.BLOCK),
    amount = 25..50
) {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "yucca_growth")

        val FEATURE: Feature<FeatureConfig> = registerFeature(
            id = IDENTIFIER,
            feature = YuccaFeature(),
            biomes = listOf(
                Biomes.SAVANNA,
                Biomes.SAVANNA_PLATEAU,
                Biomes.SHATTERED_SAVANNA,
                Biomes.SHATTERED_SAVANNA_PLATEAU
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