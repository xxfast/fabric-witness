package com.xfastgames.witness.utils

import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.world.ContainerHelper
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity

class BlockInventory(size: Int, val owner: BlockEntity) : WorldlyContainer {

    val items: NonNullList<ItemStack> = NonNullList.withSize(size, ItemStack.EMPTY)

    override fun clearContent() {
        items.clear()
    }

    override fun getContainerSize(): Int = items.size

    override fun isEmpty(): Boolean = items.all { it.isEmpty }

    override fun getItem(slot: Int): ItemStack = items[slot]

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val itemStack = ContainerHelper.takeItem(items, slot)
        setChanged()
        return itemStack
    }

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        val itemStack = ContainerHelper.removeItem(items, slot, amount)
        setChanged()
        return itemStack
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        items[slot] = stack
        if (stack.count > maxStackSize) {
            stack.count = maxStackSize
        }
        setChanged()
    }

    override fun getSlotsForFace(side: Direction): IntArray {
        return IntArray(items.size) { it }
    }

    override fun stillValid(player: Player): Boolean = true

    override fun canPlaceItemThroughFace(slot: Int, stack: ItemStack, dir: Direction?): Boolean = true

    override fun canTakeItemThroughFace(slot: Int, stack: ItemStack, dir: Direction): Boolean = true

    override fun setChanged() {
        owner.setChanged()
        if (owner is Syncable && owner.level?.isClientSide == false) owner.sync()
    }
}
