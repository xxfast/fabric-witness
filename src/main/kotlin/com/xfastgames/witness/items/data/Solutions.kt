package com.xfastgames.witness.items.data

import com.google.common.graph.Graph
import com.google.common.graph.MutableGraph
import com.xfastgames.witness.utils.guava.mutableGraph

/**
 * The verdict on a submitted line, pure so the server can reach the same answer as the client
 * (rules/minecraft/05-puzzle-frame.md, "server-authoritative solve"). The client's solver runs
 * this to give instant feedback; the server runs it again before anything in the world changes.
 */
sealed class Verdict {
    object Accepted : Verdict()

    /**
     * The path reached an end but failed. [missedHexagons] and [clashingSquares] are the symbols
     * to flash (rules 04 and 06); both empty when the line itself was malformed.
     */
    data class Rejected(
        val missedHexagons: List<Hexagon> = emptyList(),
        val clashingSquares: List<CellSymbol> = emptyList(),
    ) : Verdict()
}

/** Edges the line can never run along. `NONE` is an absent segment; `BREAK` is a gap (rule 03). */
val IMPASSABLE_EDGES: Set<Modifier> = setOf(Modifier.NONE, Modifier.BREAK)

/**
 * Rule 00: a solution is a single simple path along existing, traversable edges, from a `START`
 * node to an `END` node.
 */
fun Panel.isStructurallyValid(path: List<Node>): Boolean {
    if (path.size < 2) return false
    if (path.first().modifier != Modifier.START) return false
    if (path.last().modifier != Modifier.END) return false
    if (path.distinct().size != path.size) return false
    return path.zipWithNext().all { (from, to) ->
        graph.hasEdgeConnecting(from, to) &&
            graph.edgeValue(from, to).map { edge -> edge.modifier }.orElse(Modifier.NONE) !in IMPASSABLE_EDGES
    }
}

/**
 * Judges [path], an ordered node list from a start to an end, against every rule this mod
 * models: the line rule, hexagons, and the region symbols.
 */
fun Panel.verdict(path: List<Node>): Verdict {
    if (!isStructurallyValid(path)) return Verdict.Rejected()
    val missed: List<Hexagon> = unsatisfiedHexagons(path)
    // Region symbols (rules/witness/06-colored-squares.md) are checked against the partition the
    // finished line makes; every failing symbol is reported, not just the first rule's.
    val clashing: List<CellSymbol> = clashingSquares(path)
    if (missed.isEmpty() && clashing.isEmpty()) return Verdict.Accepted
    return Verdict.Rejected(missed, clashing)
}

/** A side of the panel, in the panel's own terms: as the composer shows it. */
enum class Side { TOP, BOTTOM, LEFT, RIGHT }

/**
 * The side(s) of the panel the used end nub points out of, which is where a solved frame sends
 * its power (rules/minecraft/05-puzzle-frame.md, "where the power goes"). A squared-off nub is
 * one side; a diagonal corner nub is two, the fork. Empty when the path does not end on a nub.
 *
 * Panel space is +y up but **-x right**: a panel is drawn mirrored on the x axis, so a nub at
 * the low-x edge is the one the player sees on the right. Do not re-derive this from the render
 * transforms; it was observed in game (2026-08-29: a nub drawn on the viewer's right came out as
 * dx < 0, and the frame on `right_connected` had to light). Which world direction a side is on a
 * given frame is the frame block's business, not the panel's.
 */
fun List<Node>.exitSides(): Set<Side> {
    if (size < 2) return emptySet()
    val nub: Node = last()
    if (nub.modifier != Modifier.END) return emptySet()
    val anchor: Node = this[lastIndex - 1]
    val dx: Float = nub.x - anchor.x
    val dy: Float = nub.y - anchor.y
    val sides: MutableSet<Side> = mutableSetOf()
    if (dx > EXIT_EPSILON) sides += Side.LEFT
    if (dx < -EXIT_EPSILON) sides += Side.RIGHT
    if (dy > EXIT_EPSILON) sides += Side.TOP
    if (dy < -EXIT_EPSILON) sides += Side.BOTTOM
    return sides
}

/**
 * Every side this panel has an end nub on: the sides power can leave by, and so the sides that
 * never take power in (rules/minecraft/05-puzzle-frame.md, "where the power goes"). Known from
 * the panel alone, before anyone traces; [exitSides] is the one of these the line actually used.
 */
fun Panel.endSides(): Set<Side> = graph.nodes()
    .filter { node -> node.modifier == Modifier.END }
    .flatMap { nub ->
        val anchor: Node = graph.adjacentNodes(nub).firstOrNull { it.modifier != Modifier.END } ?: return@flatMap emptyList()
        listOf(anchor, nub).exitSides()
    }
    .toSet()

private const val EXIT_EPSILON = 0.001f

/** The drawn line for an ordered path: every node, joined in order. */
fun List<Node>.toLine(): Graph<Node> {
    val graph: MutableGraph<Node> = mutableGraph()
    forEach(graph::addNode)
    zipWithNext().forEach { (from, to) -> graph.putEdge(from, to) }
    return graph
}

/** This panel with [line] drawn on it; everything else untouched. */
fun Panel.withLine(line: Graph<Node>): Panel = when (this) {
    is Panel.Grid -> copy(line = line)
    is Panel.Tree -> copy(line = line)
    is Panel.Freeform -> copy(line = line)
}
