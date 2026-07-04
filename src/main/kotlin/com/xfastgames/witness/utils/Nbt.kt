package com.xfastgames.witness.utils

import net.minecraft.nbt.AbstractNbtNumber
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIntArray

/**
 * Numeric NBT readers that tolerate any numeric tag type. Needed because puzzle data may arrive
 * either from persisted NBT (exact types) or from recipe/component JSON converted through dynamic
 * ops, where e.g. ints can surface as bytes/longs and floats as doubles.
 */
fun NbtCompound.getIntTolerant(key: String, default: Int = 0): Int =
    (get(key) as? AbstractNbtNumber)?.intValue() ?: default

fun NbtCompound.getFloatTolerant(key: String, default: Float = 0f): Float =
    (get(key) as? AbstractNbtNumber)?.floatValue() ?: default

/** Reads an int collection stored either as an [NbtIntArray] or as a list of numeric tags. */
fun NbtCompound.getIntListTolerant(key: String): List<Int> =
    when (val element = get(key)) {
        is NbtIntArray -> element.intArray.toList()
        else -> getListOrEmpty(key).mapNotNull { entry -> (entry as? AbstractNbtNumber)?.intValue() }
    }
