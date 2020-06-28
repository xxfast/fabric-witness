package com.xfastgames.witness.utils

import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
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
    id: Identifier
): Block =
    Registry.register(Registry.BLOCK, id, block)

fun registerBlockItem(
    block: Block,
    id: Identifier,
    settings: Item.Settings = Item.Settings().group(ItemGroup.MISC)
): BlockItem {
    return Registry.register(Registry.ITEM, id, BlockItem(block, settings))
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

inline fun <T : Entity> registerEntity(
    id: Identifier,
    crossinline typeBuilder: () -> EntityType<T>
): EntityType<T> = Registry.register(
    Registry.ENTITY_TYPE,
    id,
    typeBuilder()
)