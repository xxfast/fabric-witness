package com.xfastgames.witness.features

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.decorations.MimosaBush
import com.xfastgames.witness.utils.registerFeature
import net.minecraft.util.Identifier
import net.minecraft.world.biome.Biomes
import net.minecraft.world.gen.GenerationStep
import net.minecraft.world.gen.decorator.ChanceDecoratorConfig
import net.minecraft.world.gen.decorator.Decorator
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.FeatureConfig

class MimosaBushFeature : PatchOfBlocksFeature(listOf(MimosaBush.BLOCK)) {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "mimosa_bush_growth")

        val FEATURE: Feature<FeatureConfig> = registerFeature(
            IDENTIFIER,
            MimosaBushFeature(),
            listOf(
                Biomes.SWAMP,
                Biomes.SWAMP_HILLS
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