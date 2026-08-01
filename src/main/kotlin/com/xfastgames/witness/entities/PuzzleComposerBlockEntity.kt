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
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.storage.ReadView
import net.minecraft.storage.WriteView
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess

/** C2S payload used by the composer screen to synchronise a single inventory slot with the server. */
data class SynchronizePuzzleSlotPayload(
    val pos: BlockPos,
    val slotIndex: Int,
    val stack: ItemStack
) : CustomPayload {

    companion object {
        val ID: CustomPayload.Id<SynchronizePuzzleSlotPayload> =
            CustomPayload.Id(PuzzleComposerBlockEntity.SYNCHRONIZE_C2S_ID)

        val CODEC: PacketCodec<RegistryByteBuf, SynchronizePuzzleSlotPayload> = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, SynchronizePuzzleSlotPayload::pos,
            PacketCodecs.VAR_INT, SynchronizePuzzleSlotPayload::slotIndex,
            ItemStack.OPTIONAL_PACKET_CODEC, SynchronizePuzzleSlotPayload::stack,
            ::SynchronizePuzzleSlotPayload
        )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}

class PuzzleComposerBlockEntity(pos: BlockPos?, state: BlockState?) : BlockEntity(ENTITY_TYPE, pos, state),
    NamedScreenHandlerFactory,
    InventoryProvider,
    Syncable,
    ExtendedScreenHandlerFactory<BlockPos> {

    companion object : Clientside {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "puzzle_composer_entity")

        // client-side editor needs to send a packet to server to synchronise the client inventory with server's
        val SYNCHRONIZE_C2S_ID = Identifier.of(Witness.IDENTIFIER, "synchronise_puzzle_slot")

        /** Input + output only. Recolour is the panel dye recipe, not a composer slot. */
        const val INVENTORY_SIZE = 2

        val ENTITY_TYPE: BlockEntityType<PuzzleComposerBlockEntity> = registerBlockEntity(IDENTIFIER) {
            FabricBlockEntityTypeBuilder
                .create({ pos, state -> PuzzleComposerBlockEntity(pos, state) }, PuzzleComposerBlock.BLOCK)
                .build()
        }

        init {
            PayloadTypeRegistry.playC2S().register(SynchronizePuzzleSlotPayload.ID, SynchronizePuzzleSlotPayload.CODEC)
            ServerPlayNetworking.registerGlobalReceiver(SynchronizePuzzleSlotPayload.ID) { payload, context ->
                val entity: BlockEntity? = context.player().entityWorld.getBlockEntity(payload.pos)
                require(entity is PuzzleComposerBlockEntity)
                entity.inventory.setStack(payload.slotIndex, payload.stack)
            }
        }

        override fun onClient() {
            PuzzleComposerBlockRenderer.register()
        }
    }

    val inventory = BlockInventory(INVENTORY_SIZE, this)

    override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity?): ScreenHandler? =
        PuzzleComposerScreenDescription(syncId, inv, ScreenHandlerContext.create(world, pos))

    override fun getScreenOpeningData(player: ServerPlayerEntity): BlockPos = pos

    override fun getDisplayName(): Text = Text.translatable(cachedState.block.translationKey)

    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory = inventory

    override fun readData(view: ReadView) {
        super.readData(view)
        inventory.items.clear()
        Inventories.readData(view, inventory.items)
    }

    override fun writeData(view: WriteView) {
        super.writeData(view)
        Inventories.writeData(view, inventory.items)
    }

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener>? = BlockEntityUpdateS2CPacket.create(this)

    override fun toInitialChunkDataNbt(registries: RegistryWrapper.WrapperLookup): NbtCompound = createNbt(registries)

    /** Server-side: push the block entity state to watching clients. */
    override fun sync() {
        val world = world ?: return
        if (!world.isClient) world.updateListeners(pos, cachedState, cachedState, Block.NOTIFY_ALL)
    }

    fun syncInventorySlotTag(slotIndex: Int, itemStack: ItemStack) {
        ClientPlayNetworking.send(SynchronizePuzzleSlotPayload(pos, slotIndex, itemStack))
    }
}
