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
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.FeatureConfig

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

fun <T : FeatureConfig> registerFeature(
    name: String,
    feature: Feature<T>
) {
    val registeredFeature: Feature<T> = Registry.register(
        Registry.FEATURE,
        Identifier(WITNESS_ID, name),
        feature
    )

    if (registeredFeature is BiomeFeature)
        Registry.BIOME
            .filter { biome: Biome -> biome in registeredFeature.biomes }
            .forEach { biome -> registeredFeature.onBiome(biome) }
}