package com.xfastgames.witness.utils

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration

/** Small marker used by block entities/inventories that need to push a client sync. */
interface Syncable {
    fun sync()
}

/** Builds a fresh block [BlockBehaviour.Properties] pre-populated with the required registry key (1.21.2+). */
fun blockSettings(id: Identifier): BlockBehaviour.Properties =
    BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id))

fun registerBlock(block: Block, id: Identifier): Block =
    Registry.register(BuiltInRegistries.BLOCK, id, block)

fun registerBlockItem(
    block: Block,
    id: Identifier,
    settings: Item.Properties = Item.Properties()
): BlockItem = Registry.register(
    BuiltInRegistries.ITEM,
    id,
    BlockItem(
        block,
        settings
            .setId(ResourceKey.create(Registries.ITEM, id))
            .useBlockDescriptionPrefix()
    )
)

fun <T : BlockEntity> registerBlockEntity(
    id: Identifier,
    blockEntityType: () -> BlockEntityType<T>
): BlockEntityType<T> = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, blockEntityType())

fun <T : FeatureConfiguration> registerFeature(
    id: Identifier,
    feature: Feature<T>
): Feature<T> = Registry.register(BuiltInRegistries.FEATURE, id, feature)

inline fun <T : Entity> registerEntity(
    id: Identifier,
    crossinline typeBuilder: () -> EntityType<T>
): EntityType<T> = Registry.register(
    BuiltInRegistries.ENTITY_TYPE,
    id,
    typeBuilder()
)

fun registerItem(id: Identifier, item: Item): Item =
    Registry.register(BuiltInRegistries.ITEM, id, item)

fun itemSettings(id: Identifier): Item.Properties =
    Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))

fun registerSound(id: Identifier, event: SoundEvent): SoundEvent =
    Registry.register(BuiltInRegistries.SOUND_EVENT, id, event)

fun registerSound(id: Identifier): SoundEvent =
    Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id))
