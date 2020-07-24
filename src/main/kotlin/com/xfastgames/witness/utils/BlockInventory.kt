package com.xfastgames.witness.utils

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.Direction

interface BlockInventory : SidedInventory {
    val items: DefaultedList<ItemStack>

    override fun clear() = items.clear()
    override fun size(): Int = items.size
    override fun isEmpty(): Boolean = items.isEmpty()
    override fun getStack(slot: Int): ItemStack = items[slot]
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(items, slot)
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(items, slot, amount)

    override fun setStack(slot: Int, stack: ItemStack) {
        items[slot] = stack
        if (stack.count > maxCountPerStack) {
            stack.count = maxCountPerStack
        }
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
}