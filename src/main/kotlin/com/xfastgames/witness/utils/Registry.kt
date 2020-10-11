package com.xfastgames.witness.utils

import net.fabricmc.fabric.api.network.PacketContext
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.network.PacketByteBuf
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
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
): BlockItem = Registry.register(Registry.ITEM, id, BlockItem(block, settings))

fun <T : BlockEntity> registerBlockEntity(
    id: Identifier,
    blockEntityType: () -> BlockEntityType<T>
): BlockEntityType<T> = Registry.register(Registry.BLOCK_ENTITY_TYPE, id, blockEntityType())

fun <T : FeatureConfig> registerFeature(
    id: Identifier,
    feature: Feature<T>
): Feature<T> = Registry.register(Registry.FEATURE, id, feature)

inline fun <T : Entity> registerEntity(
    id: Identifier,
    crossinline typeBuilder: () -> EntityType<T>
): EntityType<T> = Registry.register(
    Registry.ENTITY_TYPE,
    id,
    typeBuilder()
)

fun registerItem(id: Identifier, item: Item): Item =
    Registry.register(Registry.ITEM, id, item)

fun registerC2S(id: Identifier, packetConsumer: (context: PacketContext, buffer: PacketByteBuf) -> Unit) =
    ServerSidePacketRegistry.INSTANCE.register(id, packetConsumer)

fun registerSound(id: Identifier, event: SoundEvent): SoundEvent = Registry.register(Registry.SOUND_EVENT, id, event)
fun registerSound(id: Identifier): SoundEvent = Registry.register(Registry.SOUND_EVENT, id, SoundEvent(id))