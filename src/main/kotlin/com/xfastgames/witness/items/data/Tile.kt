package com.xfastgames.witness.items.data

import net.minecraft.nbt.CompoundTag
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val KEY_START = "start"
private const val KEY_TOP = "top"
private const val KEY_LEFT = "left"
private const val KEY_BOTTOM = "bottom"
private const val KEY_RIGHT = "right"

enum class Direction { TOP, RIGHT, BOTTOM, LEFT }

enum class Line { FILLED, SHORTENED, END }

data class Tile(val isStart: Boolean, val lines: MutableMap<Direction, Line>) : MutableMap<Direction, Line> by lines {
    var left: Line? by delegate(Direction.LEFT)
    var bottom: Line? by delegate(Direction.BOTTOM)
    var right: Line? by delegate(Direction.RIGHT)
    var top: Line? by delegate(Direction.TOP)

    val center: Set<Direction> = lines.keys

    private fun delegate(direction: Direction) = object : ReadWriteProperty<Tile, Line?> {
        override fun getValue(thisRef: Tile, property: KProperty<*>): Line? = thisRef.lines[direction]
        override fun setValue(thisRef: Tile, property: KProperty<*>, value: Line?) {
            if (value != null) thisRef[direction] = value
            else lines.remove(direction)
        }
    }

    constructor(start: Boolean, top: Line?, left: Line?, bottom: Line?, right: Line?) : this(
        isStart = start,
        lines = mutableMapOf<Direction, Line>().apply {
            top?.let { put(Direction.TOP, it) }
            left?.let { put(Direction.LEFT, it) }
            right?.let { put(Direction.RIGHT, it) }
            bottom?.let { put(Direction.BOTTOM, it) }
        }
    )
}

fun CompoundTag.putTile(tile: Tile) {
    putBoolean(KEY_START, tile.isStart)
    tile.top?.let { putInt(KEY_TOP, it.ordinal) }
    tile.left?.let { putInt(KEY_LEFT, it.ordinal) }
    tile.bottom?.let { putInt(KEY_BOTTOM, it.ordinal) }
    tile.right?.let { putInt(KEY_RIGHT, it.ordinal) }
}

fun CompoundTag.getTile(): Tile =
    Tile(
        getBoolean(KEY_START),
        getInt(KEY_TOP).let { Line.values()[it] }
            .takeIf { contains(KEY_TOP) },
        getInt(KEY_LEFT).let { Line.values()[it] }
            .takeIf { contains(KEY_LEFT) },
        getInt(KEY_BOTTOM).let { Line.values()[it] }
            .takeIf { contains(KEY_BOTTOM) },
        getInt(KEY_RIGHT).let { Line.values()[it] }
            .takeIf { contains(KEY_RIGHT) }
    )
