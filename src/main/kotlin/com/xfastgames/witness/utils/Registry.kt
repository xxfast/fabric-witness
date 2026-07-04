package com.xfastgames.witness.utils

import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.FeatureConfig

/** Small marker used by block entities/inventories that need to push a client sync. */
interface Syncable {
    fun sync()
}

/** Builds a fresh block [AbstractBlock.Settings] pre-populated with the required registry key (1.21.2+). */
fun blockSettings(id: Identifier): AbstractBlock.Settings =
    AbstractBlock.Settings.create().registryKey(RegistryKey.of(RegistryKeys.BLOCK, id))

fun registerBlock(
    block: Block,
    id: Identifier
): Block =
    Registry.register(Registries.BLOCK, id, block)

fun registerBlockItem(
    block: Block,
    id: Identifier,
    settings: Item.Settings = Item.Settings()
): BlockItem = Registry.register(
    Registries.ITEM,
    id,
    BlockItem(
        block,
        settings
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, id))
            .useBlockPrefixedTranslationKey()
    )
)

fun <T : BlockEntity> registerBlockEntity(
    id: Identifier,
    blockEntityType: () -> BlockEntityType<T>
): BlockEntityType<T> = Registry.register(Registries.BLOCK_ENTITY_TYPE, id, blockEntityType())

fun <T : FeatureConfig> registerFeature(
    id: Identifier,
    feature: Feature<T>
): Feature<T> = Registry.register(Registries.FEATURE, id, feature)

inline fun <T : Entity> registerEntity(
    id: Identifier,
    crossinline typeBuilder: () -> EntityType<T>
): EntityType<T> = Registry.register(
    Registries.ENTITY_TYPE,
    id,
    typeBuilder()
)

fun registerItem(id: Identifier, item: Item): Item =
    Registry.register(Registries.ITEM, id, item)

/** Builds a fresh item [Item.Settings] pre-populated with the required registry key (1.21.2+). */
fun itemSettings(id: Identifier): Item.Settings =
    Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id))

fun registerSound(id: Identifier, event: SoundEvent): SoundEvent =
    Registry.register(Registries.SOUND_EVENT, id, event)

fun registerSound(id: Identifier): SoundEvent =
    Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id))
