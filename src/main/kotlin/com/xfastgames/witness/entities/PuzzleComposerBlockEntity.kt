package com.xfastgames.witness.entities

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.entities.renderer.PuzzleComposerBlockRenderer
import com.xfastgames.witness.screens.PuzzleScreen
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlockEntity
import com.xfastgames.witness.utils.registerC2P
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
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
import java.util.function.Supplier

class PuzzleComposerBlockEntity : BlockEntity(ENTITY_TYPE),
    NamedScreenHandlerFactory,
    InventoryProvider,
    BlockEntityClientSerializable,
    ExtendedScreenHandlerFactory {

    companion object : Clientside {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_composer_entity")

        // client-side editor needs to send a packet to server to synchronise the client inventory with server's
        val SYNCHRONIZE_C2P_ID = Identifier(Witness.IDENTIFIER, "synchronise_puzzle_slot")

        const val INVENTORY_SIZE = 7

        val ENTITY_TYPE: BlockEntityType<PuzzleComposerBlockEntity> = registerBlockEntity(IDENTIFIER) {
            BlockEntityType.Builder
                .create(Supplier { PuzzleComposerBlockEntity() }, PuzzleComposerBlock.BLOCK)
                .build(null)
        }

        init {
            registerC2P(SYNCHRONIZE_C2P_ID) { context, buffer ->
                val inventoryPos: BlockPos = buffer.readBlockPos()
                val slotIndex: Int = buffer.readInt()
                val tag: CompoundTag = requireNotNull(buffer.readCompoundTag())

                context.taskQueue.execute {
                    val entity: BlockEntity? = context.player.world.getBlockEntity(inventoryPos)
                    require(entity is PuzzleComposerBlockEntity)
                    val updatedStack: ItemStack = entity.inventory.getStack(slotIndex).apply { this.tag = tag }
                    entity.inventory.setStack(slotIndex, updatedStack)
                }
            }
        }

        override fun onClient() {
            BlockEntityRendererRegistry.INSTANCE.register(ENTITY_TYPE) { dispatcher: BlockEntityRenderDispatcher ->
                PuzzleComposerBlockRenderer(dispatcher)
            }
        }
    }

    val inventory = BlockInventory(INVENTORY_SIZE, this)

    override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity?): ScreenHandler? =
        PuzzleScreen.PuzzleScreenDescription(syncId, inv, ScreenHandlerContext.create(world, pos))

    override fun writeScreenOpeningData(player: ServerPlayerEntity?, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
    }

    override fun getDisplayName(): Text = TranslatableText(cachedState.block.translationKey)

    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory = inventory

    override fun fromTag(state: BlockState?, tag: CompoundTag) {
        super.fromTag(state, tag)
        inventory.items.clear()
        Inventories.fromTag(tag, inventory.items)
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        super.toTag(tag)
        Inventories.toTag(tag, inventory.items)
        return tag
    }

    override fun toClientTag(tag: CompoundTag): CompoundTag = toTag(tag)
    override fun fromClientTag(tag: CompoundTag) = fromTag(cachedState, tag)

    fun syncInventorySlotTag(slotIndex: Int, tag: CompoundTag) {
        val passedData = PacketByteBuf(Unpooled.buffer())
        passedData.writeBlockPos(pos)
        passedData.writeInt(slotIndex)
        passedData.writeCompoundTag(tag)
        ClientSidePacketRegistry.INSTANCE.sendToServer(SYNCHRONIZE_C2P_ID, passedData)
    }
}