package com.xfastgames.witness.utils

import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag

/**
 * Numeric NBT readers that tolerate any numeric tag type. Needed because puzzle data may arrive
 * either from persisted NBT (exact types) or from recipe/component JSON converted through dynamic
 * ops, where e.g. ints can surface as bytes/longs and floats as doubles.
 */
fun CompoundTag.getIntTolerant(key: String, default: Int = 0): Int =
    (get(key) as? NumericTag)?.intValue() ?: default

fun CompoundTag.getFloatTolerant(key: String, default: Float = 0f): Float =
    (get(key) as? NumericTag)?.floatValue() ?: default

/** Bools arrive as ByteTag (or any number from JSON). Empty → [default]. */
fun CompoundTag.getBooleanTolerant(key: String, default: Boolean = false): Boolean =
    (get(key) as? NumericTag)?.let { it.byteValue() != 0.toByte() } ?: default

/** Reads an int collection stored either as an [IntArrayTag] or as a list of numeric tags. */
fun CompoundTag.getIntListTolerant(key: String): List<Int> =
    when (val element = get(key)) {
        is IntArrayTag -> element.getAsIntArray().toList()
        else -> getListOrEmpty(key).mapNotNull { entry -> (entry as? NumericTag)?.intValue() }
    }
