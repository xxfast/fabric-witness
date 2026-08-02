package com.xfastgames.witness.utils

import net.minecraft.world.item.ItemStack

val ItemStack.isNotEmpty: Boolean get() = !this.isEmpty