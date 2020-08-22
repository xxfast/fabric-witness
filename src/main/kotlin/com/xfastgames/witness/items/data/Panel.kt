package com.xfastgames.witness.items.data

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.ListTag

private const val KEY_PANEL = "panel"
private const val KEY_SIZE = "width"
private const val KEY_TILES = "tiles"
private const val KEY_LINE = "line"

data class Panel(val tiles: List<List<Tile>>, val line: List<Float>) {
    constructor(size: Int) : this(generate(size).tiles, listOf(1.1f, 1.2f))

    fun put(x: Int, y: Int, copier: Tile.() -> Tile): Panel = copy(
        tiles = tiles.mapIndexed { xIndex, cols ->
            cols.mapIndexed { yIndex, tile ->
                if (xIndex == x && yIndex == y) copier(tile) else tile
            }
        }
    )

    fun resize(by: Int): Panel {
        val newTiles: MutableList<List<Tile>> = tiles
            .map { cols ->
                val mutableCols: MutableList<Tile> =
                    cols.map { tile -> tile.apply { right = Line.FILLED } }.toMutableList()

                repeat(by) { mutableCols.add(Tile(false, Line.FILLED, Line.FILLED, Line.FILLED, Line.FILLED)) }
                return@map mutableCols.toList()
            }
            .toMutableList()
        repeat(by) {
            newTiles.add(mutableListOf<Tile>().apply {
                repeat(height + by) { add(Tile(false, Line.FILLED, Line.FILLED, Line.FILLED, Line.FILLED)) }
            })
        }
        return copy(tiles = newTiles)
    }

    val width: Int get() = tiles.size
    val height: Int get() = tiles.maxBy { it.size }!!.size
}

private fun generate(size: Int): Panel {
    val col: MutableList<List<Tile>> = mutableListOf()
    repeat(size) { x ->
        val row: MutableList<Tile> = mutableListOf()
        repeat(size) { y ->
            row += Tile(start = false,
                top = Line.FILLED.takeIf { y != 0 },
                left = Line.FILLED.takeIf { x != 0 },
                right = Line.FILLED.takeIf { x < size - 1 },
                bottom = Line.FILLED.takeIf { y < size - 1 }
            )
        }
        col.add(row.toList())
    }
    return Panel(col.toList(), emptyList())
}

fun CompoundTag.getPanel(): Panel =
    getCompound(KEY_PANEL).let { tag ->
        Panel(
            tag.getList(KEY_TILES, 10)
                .filterIsInstance<CompoundTag>()
                .map { it.getTile() }
                .chunked(tag.getInt(KEY_SIZE)),
            tag.getList(KEY_LINE, 5)
                .filterIsInstance<FloatTag>()
                .map { it.float }
                .toList()
        )
    }

fun CompoundTag.putPanel(panel: Panel) {
    put(KEY_PANEL, CompoundTag().apply {
        put(KEY_TILES, ListTag().apply {
            panel.tiles.flatten().map { tile ->
                add(CompoundTag().apply { putTile(tile) })
            }
        })
        putInt(KEY_SIZE, panel.tiles.size)
        put(KEY_LINE, ListTag().apply {
            panel.line.forEach { point -> add(FloatTag.of(point)) }
        })
    })
}
