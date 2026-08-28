package com.xfastgames.witness.items.data

import com.xfastgames.witness.utils.getFloatTolerant
import com.xfastgames.witness.utils.getIntTolerant
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.item.DyeColor

private const val KEY_CELL_X = "x"
private const val KEY_CELL_Y = "y"
private const val KEY_CELL_FIGURE = "figure"
private const val KEY_CELL_COLOR = "color"

/**
 * What a region symbol *is*, and nothing else (rules/witness/06-colored-squares.md). Its colour, and
 * later a polyomino's shape, are attributes held on [CellSymbol] rather than values here, for the
 * same reason [Atom] keeps a hexagon's colour out of the enum. Serialized by ordinal, so append-only.
 */
enum class Figure { SQUARE }

/**
 * A symbol drawn inside a cell, the space between four grid nodes, as opposed to an [Atom] on a
 * node or an edge. ([x], [y]) is the cell's centre in panel units, the same space nodes live in.
 *
 * Held on [Panel.symbols], not in the graph: a cell is not a node, and the graph has nowhere to hang
 * one. A cell is a *position*: it exists whether or not the nodes and segments around it do, so
 * carving on the Grid tab never removes a symbol, it just merges the cell into a bigger region.
 */
data class CellSymbol(
    val x: Float,
    val y: Float,
    val figure: Figure,
    val color: DyeColor
)

fun Int.toFigure(): Figure? = Figure.values().getOrNull(this)

fun CompoundTag.getCellSymbol(): CellSymbol? {
    // A figure this version does not know reads as no symbol rather than failing the panel.
    val figure: Figure = getIntTolerant(KEY_CELL_FIGURE).toFigure() ?: return null
    return CellSymbol(
        x = getFloatTolerant(KEY_CELL_X),
        y = getFloatTolerant(KEY_CELL_Y),
        figure = figure,
        color = DyeColor.values().getOrElse(getIntTolerant(KEY_CELL_COLOR)) { DyeColor.BLACK }
    )
}

fun CompoundTag.putCellSymbol(symbol: CellSymbol) {
    putFloat(KEY_CELL_X, symbol.x)
    putFloat(KEY_CELL_Y, symbol.y)
    putInt(KEY_CELL_FIGURE, symbol.figure.ordinal)
    putInt(KEY_CELL_COLOR, symbol.color.ordinal)
}

/** Absent on every panel saved before region symbols existed, which reads as none. */
fun CompoundTag.getCellSymbols(key: String): List<CellSymbol> =
    getListOrEmpty(key)
        .filterIsInstance<CompoundTag>()
        .mapNotNull { tag -> tag.getCellSymbol() }

fun CompoundTag.putCellSymbols(key: String, symbols: List<CellSymbol>) {
    put(key, ListTag().apply {
        symbols.forEach { symbol -> add(CompoundTag().apply { putCellSymbol(symbol) }) }
    })
}
