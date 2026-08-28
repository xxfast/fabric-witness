package com.xfastgames.witness.items.data

import kotlin.math.abs

/**
 * The region partition every region symbol validates against (rules/witness/README.md, step 3):
 * the finished line cuts the panel's cells into connected regions, and a symbol is checked only
 * against the region holding its cell.
 *
 * Only the line cuts. A broken edge and an absent segment both leave the cells either side of them
 * joined (rules/witness/06-colored-squares.md#what-cuts-a-region), so the graph's edge values are
 * never consulted here, only the submitted path.
 */

/**
 * The cells of this panel grouped into regions by [path], the submitted line as an ordered node
 * list. Every cell lands in exactly one region; a panel with no cells has no regions.
 */
fun Panel.regions(path: List<Node>): List<Set<Node>> {
    val cells: List<Node> = cells()
    if (cells.isEmpty()) return emptyList()

    // Edges the line drew, as unordered node-position pairs. Positions rather than nodes so a
    // path holding the graph's marked copies still matches a cell's bare corner positions.
    val drawn: Set<Set<Pair<Float, Float>>> = path.zipWithNext()
        .map { (from, to) -> setOf(from.x to from.y, to.x to to.y) }
        .toSet()

    val parent: MutableMap<Node, Node> = cells.associateWith { it }.toMutableMap()
    fun find(cell: Node): Node {
        var root: Node = cell
        while (parent.getValue(root) != root) root = parent.getValue(root)
        return root
    }
    fun union(a: Node, b: Node) { parent[find(a)] = find(b) }

    cells.forEach { cell ->
        // Right and top neighbours only, so every side-by-side pair is considered once.
        listOf(Node(cell.x + 1f, cell.y), Node(cell.x, cell.y + 1f)).forEach { candidate ->
            val neighbour: Node = cells.find { near(it, candidate) } ?: return@forEach
            if (!drawn.contains(sharedEdge(cell, neighbour))) union(cell, neighbour)
        }
    }

    return cells.groupBy { cell -> find(cell) }.values.map { it.toSet() }
}

/** The two corner positions two side-by-side cells share, as an unordered pair. */
private fun sharedEdge(a: Node, b: Node): Set<Pair<Float, Float>> {
    val midX: Float = (a.x + b.x) / 2
    val midY: Float = (a.y + b.y) / 2
    return if (abs(a.y - b.y) < ANCHOR_EPSILON) {
        // Horizontal neighbours share the vertical edge between them.
        setOf(midX to midY - 0.5f, midX to midY + 0.5f)
    } else {
        setOf(midX - 0.5f to midY, midX + 0.5f to midY)
    }
}

private fun near(a: Node, b: Node): Boolean =
    abs(a.x - b.x) <= ANCHOR_EPSILON && abs(a.y - b.y) <= ANCHOR_EPSILON

/**
 * The squares [path] leaves in a region holding more than one colour, empty when the square rule
 * passes. Every square in such a region is at fault, the majority colour included: each one
 * individually has a differently coloured neighbour in its region
 * (rules/witness/06-colored-squares.md#edge-cases).
 */
fun Panel.clashingSquares(path: List<Node>): List<CellSymbol> {
    val squares: List<CellSymbol> = symbols.filter { it.figure == Figure.SQUARE }
    if (squares.isEmpty()) return emptyList()

    return regions(path).flatMap { region ->
        val inside: List<CellSymbol> = squares.filter { square ->
            region.any { cell -> abs(cell.x - square.x) <= ANCHOR_EPSILON && abs(cell.y - square.y) <= ANCHOR_EPSILON }
        }
        if (inside.distinctBy { it.color }.size > 1) inside else emptyList()
    }
}
