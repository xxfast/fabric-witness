package com.xfastgames.witness.utils

import com.xfastgames.witness.WITNESS_ID
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.block.Block
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import net.minecraft.world.biome.Biome
import net.minecraft.world.gen.GenerationStep
import net.minecraft.world.gen.decorator.ChanceDecoratorConfig
import net.minecraft.world.gen.decorator.Decorator
import net.minecraft.world.gen.feature.DefaultFeatureConfig
import net.minecraft.world.gen.feature.Feature

fun registerBlock(
    block: Block,
    name: String,
    render: RenderLayer? = null,
    settings: Item.Settings = Item.Settings().group(ItemGroup.MISC)
) {
    val id = Identifier(WITNESS_ID, name)
    Registry.register(Registry.BLOCK, id, block)
    Registry.register(Registry.ITEM, id, BlockItem(block, settings))
    render?.let { BlockRenderLayerMap.INSTANCE.putBlock(block, it) }
}

fun registerFeature(
    name: String,
    feature: Feature<DefaultFeatureConfig?>,
    step: GenerationStep.Feature
) {
    val registeredFeature = Registry.register(
        Registry.FEATURE,
        Identifier(WITNESS_ID, name),
        feature
    )

    if (feature is BiomeFeature)
        Registry.BIOME
            .filter { biome: Biome -> biome in feature.biomes }
            .forEach { biome ->
                biome.addFeature(
                    step,
                    registeredFeature.configure(DefaultFeatureConfig())
                        .createDecoratedFeature(Decorator.CHANCE_HEIGHTMAP.configure(ChanceDecoratorConfig(100)))
                )
            }
}