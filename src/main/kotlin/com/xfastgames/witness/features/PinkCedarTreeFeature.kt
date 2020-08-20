package com.xfastgames.witness.features

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.building.CedarLog
import com.xfastgames.witness.blocks.decorations.PinkCedarLeaves
import com.xfastgames.witness.features.decorators.BougainvilleaTreeDecorator
import com.xfastgames.witness.utils.BiomeFeature
import com.xfastgames.witness.utils.registerFeature
import net.minecraft.util.Identifier
import net.minecraft.world.biome.Biome
import net.minecraft.world.gen.UniformIntDistribution
import net.minecraft.world.gen.decorator.ConfiguredDecorator
import net.minecraft.world.gen.decorator.CountExtraDecoratorConfig
import net.minecraft.world.gen.decorator.Decorator
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.TreeFeature
import net.minecraft.world.gen.feature.TreeFeatureConfig
import net.minecraft.world.gen.feature.size.TwoLayersFeatureSize
import net.minecraft.world.gen.foliage.BlobFoliagePlacer
import net.minecraft.world.gen.stateprovider.SimpleBlockStateProvider
import net.minecraft.world.gen.trunk.StraightTrunkPlacer

// TODO: This seems to be broken
object PinkCedarTreeFeature : BiomeFeature<TreeFeatureConfig, CountExtraDecoratorConfig>() {

    val IDENTIFIER = Identifier(Witness.IDENTIFIER, "pink_cedar_trees")

    override val biomes: List<Biome> = emptyList()

    override val feature: Feature<TreeFeatureConfig> =
        registerFeature(IDENTIFIER, TreeFeature(TreeFeatureConfig.CODEC))

    override val configuration: TreeFeatureConfig = TreeFeatureConfig
        .Builder(
            SimpleBlockStateProvider(CedarLog.BLOCK.defaultState),
            SimpleBlockStateProvider(PinkCedarLeaves.BLOCK.defaultState),
            BlobFoliagePlacer(
                UniformIntDistribution.of(2),
                UniformIntDistribution.of(0),
                0
            ),
            StraightTrunkPlacer(5, 2, 0),
            TwoLayersFeatureSize(1, 0, 1)
        )
        .decorators(listOf(BougainvilleaTreeDecorator()))
        .ignoreVines()
        .build()

    override val decorator: ConfiguredDecorator<CountExtraDecoratorConfig> =
        Decorator.COUNT_EXTRA.configure(CountExtraDecoratorConfig(1, 0.1f, 0))
}