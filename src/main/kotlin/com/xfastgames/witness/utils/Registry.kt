package com.xfastgames.witness.utils

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
    id: Identifier,
    render: RenderLayer? = null
): Block {
    val registeredBlock: Block = Registry.register(Registry.BLOCK, id, block)
    render?.let { BlockRenderLayerMap.INSTANCE.putBlock(registeredBlock, it) }
    return block
}

fun registerBlockItem(
    block: Block,
    id: Identifier,
    render: RenderLayer? = null,
    settings: Item.Settings = Item.Settings().group(ItemGroup.MISC)
): BlockItem {
    val blockItem: BlockItem = Registry.register(Registry.ITEM, id, BlockItem(block, settings))
    render?.let { BlockRenderLayerMap.INSTANCE.putBlock(block, it) }
    return blockItem
}

// TODO: Refactor [onBiome] lambda
fun <T : FeatureConfig> registerFeature(
    id: Identifier,
    feature: Feature<T>,
    biomes: List<Biome> = emptyList(),
    onBiome: (registeredFeature: Feature<T>, biome: Biome) -> Unit
): Feature<T> {
    val registeredFeature: Feature<T> =
        Registry.register(Registry.FEATURE, id, feature)

    Registry.BIOME
        .filter { biome: Biome -> biome in biomes }
        .forEach { biome -> onBiome(registeredFeature, biome) }

    return registeredFeature
}