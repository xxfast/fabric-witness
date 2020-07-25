package com.xfastgames.witness.utils

import net.minecraft.entity.player.PlayerInventory

fun PlayerInventory.isFull(): Boolean = emptySlot == -1