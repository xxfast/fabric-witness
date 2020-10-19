package com.xfastgames.witness.items.data

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.ListTag
import net.minecraft.util.DyeColor

private const val KEY_PANEL = "panel"
private const val KEY_SIZE = "width"
private const val KEY_TILES = "tiles"
private const val KEY_LINE = "line"
private const val KEY_BACKGROUND_COLOR = "backgroundColor"

data class Panel(
    val tiles: List<List<Tile>>,
    val line: List<Float>,
    val backgroundColor: DyeColor
) {

    companion object {
        val DEFAULT: Panel = ofSize(3)
        fun ofSize(size: Int): Panel = generate(size)
    }

    fun put(x: Int, y: Int, copier: Tile.() -> Tile): Panel = copy(
        tiles = tiles.mapIndexed { xIndex, cols ->
            cols.mapIndexed { yIndex, tile ->
                if (xIndex == x && yIndex == y) copier(tile) else tile
            }
        }
    )

    private fun grow(by: Int): Panel {
        if (by <= 0) return this

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

    private fun shrink(by: Int): Panel {
        if (by <= 0) return this
        val removedColumns: List<List<Tile>> = tiles.dropLast(by)
        val removedRows: List<List<Tile>> = removedColumns
            .mapIndexed { colIndex, col ->
                val removedTiles: List<Tile> = col.dropLast(by)
                removedTiles
                    .mapIndexed { rowIndex, tile ->
                        tile.apply {
                            right = tile.right.takeIf { colIndex != removedColumns.lastIndex }
                            bottom = tile.bottom.takeIf { rowIndex != removedTiles.lastIndex }
                        }
                    }
            }

        return copy(tiles = removedRows)
    }

    fun resize(length: Int): Panel =
        if (length > width) grow(length - width)
        else shrink(width - length)

    val width: Int get() = tiles.size
    val height: Int get() = tiles.maxByOrNull { it.size }!!.size
}

@Suppress("UnstableApiUsage")
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
    return Panel(
        tiles = col.toList(),
        line = emptyList(),
        backgroundColor = DyeColor.WHITE
    )
}

@Suppress("UnstableApiUsage")
fun CompoundTag.getPanel(): Panel =
    getCompound(KEY_PANEL).let { tag ->
        Panel(
            tiles = tag.getList(KEY_TILES, 10)
                .filterIsInstance<CompoundTag>()
                .map { it.getTile() }
                .chunked(tag.getInt(KEY_SIZE)),
            line = tag.getList(KEY_LINE, 5)
                .filterIsInstance<FloatTag>()
                .map { it.float }
                .toList(),
            backgroundColor = DyeColor.values()[tag.getInt(KEY_BACKGROUND_COLOR)]
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
        putInt(KEY_BACKGROUND_COLOR, panel.backgroundColor.ordinal)
    })
}
