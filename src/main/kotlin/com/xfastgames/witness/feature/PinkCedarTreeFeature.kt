package com.xfastgames.witness.feature

import com.xfastgames.witness.WITNESS_ID
import com.xfastgames.witness.blocks.leaves.PinkCedarLeaves
import com.xfastgames.witness.blocks.logs.CedarLog
import com.xfastgames.witness.feature.decorators.BougainvilleaTreeDecorator
import com.xfastgames.witness.utils.registerFeature
import net.minecraft.util.Identifier
import net.minecraft.world.biome.Biomes
import net.minecraft.world.gen.GenerationStep
import net.minecraft.world.gen.decorator.CountExtraChanceDecoratorConfig
import net.minecraft.world.gen.decorator.Decorator
import net.minecraft.world.gen.feature.BranchedTreeFeatureConfig
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.OakTreeFeature
import net.minecraft.world.gen.foliage.BlobFoliagePlacer
import net.minecraft.world.gen.stateprovider.SimpleBlockStateProvider
import java.util.function.Function

class PinkCedarTreeFeature : OakTreeFeature(Function { CONFIG }) {

    companion object {
        val CONFIG: BranchedTreeFeatureConfig =
            BranchedTreeFeatureConfig
                .Builder(
                    SimpleBlockStateProvider(CedarLog.BLOCK.defaultState),
                    SimpleBlockStateProvider(PinkCedarLeaves.BLOCK.defaultState),
                    BlobFoliagePlacer(2, 0)
                )
                .treeDecorators(listOf(BougainvilleaTreeDecorator()))
                .baseHeight(5)
                .heightRandA(2)
                .foliageHeight(3)
                .noVines()
                .build()

        val IDENTIFIER = Identifier(WITNESS_ID, "pink_cedar_trees")

        val FEATURE: Feature<BranchedTreeFeatureConfig> = registerFeature(
            IDENTIFIER,
            PinkCedarTreeFeature(),
            listOf(Biomes.FLOWER_FOREST)
        ) { registeredFeature, biome ->
            biome.addFeature(
                GenerationStep.Feature.VEGETAL_DECORATION, registeredFeature
                    .configure(CONFIG)
                    .createDecoratedFeature(
                        Decorator.COUNT_EXTRA_HEIGHTMAP.configure(
                            CountExtraChanceDecoratorConfig(1, 0.1f, 0)
                        )
                    )
            )
        }
    }

}