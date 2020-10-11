package com.xfastgames.witness.utils

import net.minecraft.item.ItemStack

val ItemStack.isNotEmpty: Boolean get() = !this.isEmpty