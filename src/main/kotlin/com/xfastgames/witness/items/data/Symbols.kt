@file:Suppress("UnstableApiUsage")

package com.xfastgames.witness.items.data

import com.google.common.graph.EndpointPair
import com.google.common.graph.Graphs
import com.google.common.graph.MutableValueGraph
import net.minecraft.world.item.DyeColor
import kotlin.math.abs

/**
 * Authoring symbols onto a panel (rules/witness/04-hexagon-dots.md).
 *
 * A symbol is held beside a node's role and an edge's traversal state, so placing one never
 * disturbs either: a start point keeps starting, a hidden edge stays hidden.
 */

/**
 * Replaces [node] with [replacement], keeping every edge that touched it.
 *
 * [Node] is a data class and the graph's key, so changing any field makes a different node. It has
 * to be removed and re-added with its neighbourhood re-linked rather than mutated in place.
 */
fun Panel.withNodeReplaced(node: Node, replacement: Node): Panel {
    val updated: MutableValueGraph<Node, Edge> = Graphs.copyOf(graph)
    val neighbourhood: Map<Node, Edge> = graph.adjacentNodes(node)
        .mapNotNull { neighbour ->
            graph.edgeValue(neighbour, node).orElse(null)?.let { edge -> neighbour to edge }
        }
        .toMap()
    updated.removeNode(node)
    updated.addNode(replacement)
    neighbourhood.forEach { (neighbour, edge) -> updated.putEdgeValue(neighbour, replacement, edge) }
    return withGraph(updated)
}

/**
 * The colours the square tool cycles through, in click order, before a further click removes the
 * square (rules/witness/06-colored-squares.md#authoring). Black and white only until the rail
 * grows a palette; any other dye is legal data, just not authorable yet.
 */
val SQUARE_PALETTE: List<DyeColor> = listOf(DyeColor.BLACK, DyeColor.WHITE)

/**
 * The square tool's click at panel position ([x], [y]): places a black square in an empty cell,
 * advances an existing square along [SQUARE_PALETTE], and removes one at the end of the palette.
 *
 * Null when the position is not inside any cell of this panel, which is every position on a type
 * with no cells (a tree), so the tool refuses rather than guessing.
 */
fun Panel.withSquareCycled(x: Float, y: Float): Panel? {
    val cell: Node = cellAt(x, y) ?: return null
    val existing: CellSymbol? = symbolAt(cell.x, cell.y)
    val others: List<CellSymbol> = symbols - listOfNotNull(existing)

    if (existing == null) {
        return withSymbols(others + CellSymbol(cell.x, cell.y, Figure.SQUARE, SQUARE_PALETTE.first()))
    }
    // A square carrying a colour outside the palette (authored elsewhere) restarts the cycle,
    // so the tool can always reach the removal click from any state.
    val next: DyeColor? = SQUARE_PALETTE.getOrNull(SQUARE_PALETTE.indexOf(existing.color) + 1)
    return withSymbols(if (next == null) others else others + existing.copy(color = next))
}

/** The symbol in the cell centred at ([x], [y]), if any. */
fun Panel.symbolAt(x: Float, y: Float): CellSymbol? =
    symbols.find { symbol -> abs(symbol.x - x) <= ANCHOR_EPSILON && abs(symbol.y - y) <= ANCHOR_EPSILON }

/**
 * Toggles [symbol] on the clicked [node], or failing that on the clicked [edge].
 *
 * A node wins over an edge when both match, since a node sits on the edges that meet it and would
 * otherwise be unclickable. Returns null when nothing can take the symbol, so the caller can leave
 * the panel alone:
 *
 * - neither a node nor an edge was clicked;
 * - the edge is broken. The line can never reach the middle of a gap, so a hexagon there would make
 *   the panel unsolvable by construction (rules/witness/03-broken-edges.md).
 *
 * A start or end node accepts one. It is trivially satisfied and so carries no information, but it
 * is legal, and refusing it would be a rule this mod invented.
 */
fun Panel.withSymbolToggled(
    node: Node?,
    edge: EndpointPair<Node>?,
    symbol: Atom = Atom.HEXAGON
): Panel? {
    if (node != null) {
        val next: Atom = if (node.symbol == symbol) Atom.NONE else symbol
        return withNodeReplaced(node, node.copy(symbol = next))
    }

    if (edge == null) return null
    val current: Edge = graph.edgeValue(edge.nodeU(), edge.nodeV()).orElse(null) ?: return null
    if (current.modifier == Modifier.BREAK) return null

    val next: Atom = if (current.symbol == symbol) Atom.NONE else symbol
    val updated: MutableValueGraph<Node, Edge> = Graphs.copyOf(graph)
    updated.putEdgeValue(edge.nodeU(), edge.nodeV(), current.copy(symbol = next))
    return withGraph(updated)
}
