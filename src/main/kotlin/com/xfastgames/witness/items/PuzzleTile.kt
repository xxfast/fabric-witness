package com.xfastgames.witness.items

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.renderer.PuzzleTileItemRenderer
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerItem
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier
import kotlin.random.Random


private const val KEY_START = "start"
private const val KEY_TOP = "top"
private const val KEY_LEFT = "left"
private const val KEY_BOTTOM = "bottom"
private const val KEY_RIGHT = "right"

enum class Direction { TOP, RIGHT, BOTTOM, LEFT }

enum class Line { FILLED, SHORTENED, END }

data class Tile(val isStart: Boolean, val lines: Map<Direction, Line>) {
    val left: Line? get() = lines[Direction.LEFT]
    val bottom: Line? get() = lines[Direction.BOTTOM]
    val right: Line? get() = lines[Direction.RIGHT]
    val top: Line? get() = lines[Direction.TOP]
    val center: Set<Direction> = lines.keys

    constructor(start: Boolean, top: Line?, left: Line?, bottom: Line?, right: Line?) : this(
        isStart = start,
        lines = mapOf(
            Direction.TOP to top,
            Direction.LEFT to left,
            Direction.RIGHT to right,
            Direction.BOTTOM to bottom
        ).mapNotNull {
            it.value?.let { value -> it.key to value }
        }.toMap()
    )

    constructor() : this(
        isStart = Random.nextBoolean(),
        lines = mapOf(
            Direction.TOP to Line.values().random(),
            Direction.RIGHT to Line.values().random(),
            Direction.BOTTOM to Line.values().random(),
            Direction.LEFT to Line.values().random()
        )
    )

    fun asItemStack(): ItemStack = ItemStack(PuzzleTile.ITEM).apply { orCreateTag?.putTile(this@Tile) }
}

fun CompoundTag.putTile(tile: Tile) {
    putBoolean(KEY_START, tile.isStart)
    tile.top?.let { putInt(KEY_TOP, it.ordinal) }
    tile.left?.let { putInt(KEY_LEFT, it.ordinal) }
    tile.bottom?.let { putInt(KEY_BOTTOM, it.ordinal) }
    tile.right?.let { putInt(KEY_RIGHT, it.ordinal) }
}

fun CompoundTag.getTile(): Tile = Tile(
    getBoolean(KEY_START),
    getInt(KEY_TOP).let { Line.values()[it] },
    getInt(KEY_LEFT).let { Line.values()[it] },
    getInt(KEY_BOTTOM).let { Line.values()[it] },
    getInt(KEY_RIGHT).let { Line.values()[it] }
)

class PuzzleTile : Item(Settings().group(ItemGroup.REDSTONE)), Clientside {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_tile")
        val ITEM: Item = registerItem(IDENTIFIER, PuzzleTile())
        val RENDERER = PuzzleTileItemRenderer()
    }

    override fun onClient() {
        BuiltinItemRendererRegistry.INSTANCE.register(ITEM, RENDERER)
    }
}
