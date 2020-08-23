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

    fun grow(by: Int): Panel {
        require(by > 0)

        val mutableTiles: MutableList<MutableList<Tile>> =
            tiles.map { column -> column.toMutableList() }.toMutableList()

        mutableTiles.forEachIndexed { index, row ->
            // Connect the bottoms and rights
            val lastTile: Tile = row.removeAt(row.lastIndex)
            val updatedLastTile: Tile = lastTile.apply { bottom = Line.FILLED }
            row.add(updatedLastTile)
            if (index == mutableTiles.lastIndex) {
                row.forEach { tile -> tile.right = Line.FILLED }
            }
            // Add additional cell(s)
            val isStartingCol: Boolean = index == 0
            repeat(by) { rowIndex ->
                val isLastRow: Boolean = rowIndex == by - 1
                row.add(
                    Tile(
                        start = false,
                        top = Line.FILLED,
                        left = Line.FILLED.takeIf { !isStartingCol },
                        bottom = Line.FILLED.takeIf { !isLastRow },
                        right = Line.FILLED
                    )
                )
            }
        }

        val extrudeHeight = height + by
        repeat(by) { index ->
            val isLastCol: Boolean = index == by - 1
            val newRow: MutableList<Tile> = mutableListOf()
            repeat(extrudeHeight) { rowIndex ->
                val isFirstRow: Boolean = rowIndex == 0
                val isLastRow: Boolean = rowIndex == extrudeHeight - 1
                newRow.add(
                    Tile(
                        start = false,
                        top = Line.FILLED.takeIf { !isFirstRow },
                        left = Line.FILLED,
                        bottom = Line.FILLED.takeIf { !isLastRow },
                        right = Line.FILLED.takeIf { !isLastCol }
                    )
                )
            }
            mutableTiles.add(newRow)
        }
        return copy(tiles = mutableTiles)
    }

    fun shrink(by: Int): Panel {
        val removedColumns: List<List<Tile>> = tiles.dropLast(by)
        val removedRows: List<List<Tile>> = removedColumns
            .mapIndexed { colIndex, col ->
                val removedTiles: List<Tile> = col.dropLast(by)
                removedTiles
                    .mapIndexed { rowIndex, tile ->
                        tile.apply {
                            right = Line.FILLED.takeIf { colIndex != removedColumns.lastIndex }
                            bottom = Line.FILLED.takeIf { rowIndex != removedTiles.lastIndex }
                        }
                    }
            }

        return copy(tiles = removedRows)
    }

    fun resize(length: Int): Panel =
        if (length > width) grow(length - width)
        else shrink(length - width)

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
