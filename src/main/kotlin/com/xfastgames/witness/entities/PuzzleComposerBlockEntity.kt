package com.xfastgames.witness.entities

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.entities.renderer.PuzzleComposerBlockRenderer
import com.xfastgames.witness.screens.composer.PuzzleComposerScreenDescription
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.Syncable
import com.xfastgames.witness.utils.registerBlockEntity
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.WorldlyContainerHolder
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/** C2S payload used by the composer screen to synchronise a single inventory slot with the server. */
data class SynchronizePuzzleSlotPayload(
    val pos: BlockPos,
    val slotIndex: Int,
    val stack: ItemStack
) : CustomPacketPayload {

    companion object {
        val ID: CustomPacketPayload.Type<SynchronizePuzzleSlotPayload> =
            CustomPacketPayload.Type(PuzzleComposerBlockEntity.SYNCHRONIZE_C2S_ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SynchronizePuzzleSlotPayload> = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SynchronizePuzzleSlotPayload::pos,
            ByteBufCodecs.VAR_INT, SynchronizePuzzleSlotPayload::slotIndex,
            ItemStack.OPTIONAL_STREAM_CODEC, SynchronizePuzzleSlotPayload::stack,
            ::SynchronizePuzzleSlotPayload
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}

class PuzzleComposerBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ENTITY_TYPE, pos, state),
    MenuProvider,
    WorldlyContainerHolder,
    Syncable,
    ExtendedMenuProvider<BlockPos> {

    companion object : Clientside {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "puzzle_composer_entity")

        val SYNCHRONIZE_C2S_ID = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "synchronise_puzzle_slot")

        /** ClientInput + output only. Recolour is the panel dye recipe, not a composer slot. */
        const val INVENTORY_SIZE = 2

        val ENTITY_TYPE: BlockEntityType<PuzzleComposerBlockEntity> = registerBlockEntity(IDENTIFIER) {
            FabricBlockEntityTypeBuilder
                .create(::PuzzleComposerBlockEntity, PuzzleComposerBlock.BLOCK)
                .build()
        }

        init {
            PayloadTypeRegistry.serverboundPlay().register(SynchronizePuzzleSlotPayload.ID, SynchronizePuzzleSlotPayload.CODEC)
            ServerPlayNetworking.registerGlobalReceiver(SynchronizePuzzleSlotPayload.ID) { payload, context ->
                val entity: BlockEntity? = context.player().level().getBlockEntity(payload.pos)
                require(entity is PuzzleComposerBlockEntity)
                entity.inventory.setItem(payload.slotIndex, payload.stack)
            }
        }

        override fun onClient() {
            PuzzleComposerBlockRenderer.register()
        }
    }

    val inventory = BlockInventory(INVENTORY_SIZE, this)

    override fun createMenu(syncId: Int, inv: Inventory, player: Player): AbstractContainerMenu? {
        val level = level ?: return null
        return PuzzleComposerScreenDescription(syncId, inv, ContainerLevelAccess.create(level, blockPos))
    }

    override fun getScreenOpeningData(player: ServerPlayer): BlockPos = blockPos

    override fun getDisplayName(): Component = Component.translatable(blockState.block.descriptionId)

    override fun getContainer(state: BlockState, world: LevelAccessor, pos: BlockPos): WorldlyContainer = inventory

    override fun loadAdditional(view: ValueInput) {
        super.loadAdditional(view)
        inventory.items.clear()
        ContainerHelper.loadAllItems(view, inventory.items)
    }

    override fun saveAdditional(view: ValueOutput) {
        super.saveAdditional(view)
        ContainerHelper.saveAllItems(view, inventory.items)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag =
        saveWithoutMetadata(registries)

    /** Server-side: push the block entity state to watching clients. */
    override fun sync() {
        val level = level ?: return
        if (!level.isClientSide) {
            level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_ALL)
        }
    }

    fun syncInventorySlotTag(slotIndex: Int, itemStack: ItemStack) {
        ClientPlayNetworking.send(SynchronizePuzzleSlotPayload(blockPos, slotIndex, itemStack))
    }
}
