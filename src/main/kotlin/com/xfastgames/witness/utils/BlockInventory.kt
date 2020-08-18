package com.xfastgames.witness.utils

import com.xfastgames.witness.Witness
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction


class BlockInventory(size: Int, private val owner: BlockEntity) : SidedInventory {

    companion object {
        val SYNCHRONIZE_C2P_ID = Identifier(Witness.IDENTIFIER, "synchronise_block_inventory")

        init {
            registerC2P(SYNCHRONIZE_C2P_ID) { context, buffer ->
                val inventoryPos: BlockPos = buffer.readBlockPos()
                val tag: CompoundTag = requireNotNull(buffer.readCompoundTag())

                context.taskQueue.execute {
                    val entity: BlockEntity? = context.player.world.getBlockEntity(inventoryPos)
                    require(entity is BlockEntityClientSerializable)
                    require(entity is InventoryProvider)

                    val inventory: SidedInventory =
                        entity.getInventory(entity.cachedState, context.player.world, inventoryPos)

                    require(inventory is BlockInventory)
                    inventory.clear()
                    Inventories.fromTag(tag, inventory.items)
                }
            }
        }
    }

    val items: DefaultedList<ItemStack> = DefaultedList.ofSize(size, ItemStack.EMPTY)

    override fun clear() = items.clear()
    override fun size(): Int = items.size
    override fun isEmpty(): Boolean = items.isEmpty()
    override fun getStack(slot: Int): ItemStack = items[slot]
    override fun removeStack(slot: Int): ItemStack {
        val itemStack: ItemStack = Inventories.removeStack(items, slot)
        markDirty()
        return itemStack
    }

    override fun removeStack(slot: Int, amount: Int): ItemStack {
        val itemStack: ItemStack = Inventories.splitStack(items, slot, amount)
        markDirty()
        return itemStack
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        items[slot] = stack
        if (stack.count > maxCountPerStack) {
            stack.count = maxCountPerStack
        }
        markDirty()
    }

    override fun getAvailableSlots(side: Direction?): IntArray {
        // Just return an array of all slots
        val result = IntArray(items.size)
        for (i in result.indices) {
            result[i] = i
        }

        return result
    }

    override fun canPlayerUse(player: PlayerEntity?): Boolean = true
    override fun canInsert(slot: Int, stack: ItemStack?, dir: Direction?): Boolean = true

    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?): Boolean = true

    override fun markDirty() {
        owner.markDirty()
        require(owner is BlockEntityClientSerializable)
        if (owner.world?.isClient == false) owner.sync()
        // TODO: Looks like the inventories have their own internal mechanisms to synchronise, and this somehow seems to
        // compete with it.
        else {
            val passedData = PacketByteBuf(Unpooled.buffer())
            passedData.writeBlockPos(owner.pos)
            val inventoryTag: CompoundTag = Inventories.toTag(CompoundTag(), this.items)
            passedData.writeCompoundTag(inventoryTag)
            ClientSidePacketRegistry.INSTANCE.sendToServer(SYNCHRONIZE_C2P_ID, passedData)
        }
    }
}