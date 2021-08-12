package com.xfastgames.witness.entities

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.entities.renderer.PuzzleComposerBlockRenderer
import com.xfastgames.witness.screens.composer.PuzzleComposerScreenDescription
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlockEntity
import com.xfastgames.witness.utils.registerC2S
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
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
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess

class PuzzleComposerBlockEntity(pos: BlockPos?, state: BlockState?) : BlockEntity(ENTITY_TYPE, pos, state),
    NamedScreenHandlerFactory,
    InventoryProvider,
    BlockEntityClientSerializable,
    ExtendedScreenHandlerFactory {

    companion object : Clientside {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_composer_entity")

        // client-side editor needs to send a packet to server to synchronise the client inventory with server's
        val SYNCHRONIZE_C2S_ID = Identifier(Witness.IDENTIFIER, "synchronise_puzzle_slot")

        const val INVENTORY_SIZE = 10

        val ENTITY_TYPE: BlockEntityType<PuzzleComposerBlockEntity> = registerBlockEntity(IDENTIFIER) {
            BlockEntityType.Builder
                .create({ pos, state -> PuzzleComposerBlockEntity(pos, state) }, PuzzleComposerBlock.BLOCK)
                .build(null)
        }

        init {
            registerC2S(SYNCHRONIZE_C2S_ID) { context, buffer ->
                val inventoryPos: BlockPos = buffer.readBlockPos()
                val slotIndex: Int = buffer.readInt()
                val itemStack: ItemStack = buffer.readItemStack()
                context.taskQueue.execute {
                    val entity: BlockEntity? = context.player.world.getBlockEntity(inventoryPos)
                    require(entity is PuzzleComposerBlockEntity)
                    entity.inventory.setStack(slotIndex, itemStack)
                }
            }
        }

        override fun onClient() {
            BlockEntityRendererRegistry.INSTANCE.register(ENTITY_TYPE) { PuzzleComposerBlockRenderer() }
        }
    }

    val inventory = BlockInventory(INVENTORY_SIZE, this)

    override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity?): ScreenHandler? =
        PuzzleComposerScreenDescription(syncId, inv, ScreenHandlerContext.create(world, pos))

    override fun writeScreenOpeningData(player: ServerPlayerEntity?, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
    }

    override fun getDisplayName(): Text = TranslatableText(cachedState.block.translationKey)

    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory = inventory

    override fun readNbt(nbt: NbtCompound?) {
        super.readNbt(nbt)
        inventory.items.clear()
        Inventories.readNbt(nbt, inventory.items)
    }

    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory.items)
        return nbt
    }

    override fun toClientTag(nbt: NbtCompound): NbtCompound = writeNbt(nbt)
    override fun fromClientTag(nbt: NbtCompound) = readNbt(nbt)

    fun syncInventorySlotTag(slotIndex: Int, itemStack: ItemStack) {
        val passedData = PacketByteBuf(Unpooled.buffer())
        passedData.writeBlockPos(pos)
        passedData.writeInt(slotIndex)
        passedData.writeItemStack(itemStack)
        ClientSidePacketRegistry.INSTANCE.sendToServer(SYNCHRONIZE_C2S_ID, passedData)
    }
}