@file:Suppress("UnstableApiUsage")

package com.xfastgames.witness.items.data

import com.google.common.graph.Graphs
import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * End points (rules/witness/02-end-points.md).
 *
 * An end point is a short nub protruding from the lattice: its own `END` node, connected to a
 * border node by one ordinary edge. It is not a flag on the border node itself, so the final
 * segment into the nub is a real edge that the line has to be drawn along.
 */

/** Nub length in panel units, one line thickness (`4.pc`). Fits the half unit border margin. */
const val END_POINT_LENGTH = 0.25f

private const val BORDER_EPSILON = 0.001f

/**
 * Which way out of the panel [node] sits, as a sign per axis, or null when [node] is not on the
 * lattice border. A corner has both axes set. Existing nubs are excluded from the bounds so they
 * don't push the border outward.
 */
private fun Panel.borderSigns(node: Node): Pair<Float, Float>? {
    val lattice: List<Node> = graph.nodes().filter { it.modifier != Modifier.END }
    if (node !in lattice) return null
    val minX: Float = lattice.minOf { it.x }
    val maxX: Float = lattice.maxOf { it.x }
    val minY: Float = lattice.minOf { it.y }
    val maxY: Float = lattice.maxOf { it.y }

    val directionX: Float = when {
        node.x <= minX + BORDER_EPSILON -> -1f
        node.x >= maxX - BORDER_EPSILON -> 1f
        else -> 0f
    }
    val directionY: Float = when {
        node.y <= minY + BORDER_EPSILON -> -1f
        node.y >= maxY - BORDER_EPSILON -> 1f
        else -> 0f
    }
    if (directionX == 0f && directionY == 0f) return null
    return directionX to directionY
}

/**
 * The orientations a nub on [node] can take, in the order clicking cycles them: a corner starts
 * diagonal and can be squared off along either of its two borders, everything else has one way to
 * point. Empty when [node] can't carry an end point at all.
 */
fun Panel.endPointOrientations(node: Node): List<Pair<Float, Float>> {
    val (signX: Float, signY: Float) = borderSigns(node) ?: return emptyList()
    if (signX == 0f || signY == 0f) return listOf(signX to signY)
    val diagonal: Float = 1f / sqrt(2f)
    return listOf(
        signX * diagonal to signY * diagonal,
        0f to signY,
        signX to 0f
    )
}

private fun Node.nubAt(orientation: Pair<Float, Float>): Node = Node(
    x = x + orientation.first * END_POINT_LENGTH,
    y = y + orientation.second * END_POINT_LENGTH,
    modifier = Modifier.END
)

/** Which of [endPointOrientations] the nub [nub] on [node] is currently in, or -1 if none match. */
private fun List<Pair<Float, Float>>.indexOfNub(node: Node, nub: Node): Int = indexOfFirst { orientation ->
    val candidate: Node = node.nubAt(orientation)
    abs(candidate.x - nub.x) <= BORDER_EPSILON && abs(candidate.y - nub.y) <= BORDER_EPSILON
}

/** The nub hanging off [node], if it has one. */
fun Panel.endPointOf(node: Node): Node? =
    graph.adjacentNodes(node).firstOrNull { it.modifier == Modifier.END }

/**
 * Advances [node] through its end point cycle: no nub, then each of [endPointOrientations] in
 * turn, then back to no nub. On anything but a corner that is a plain on/off toggle. Clicking a
 * nub directly removes it.
 *
 * Returns null when [node] can't carry an end point, so the caller can leave the panel alone.
 */
fun Panel.withEndPointToggled(node: Node): Panel? {
    val graph: MutableValueGraph<Node, Edge> = Graphs.copyOf(graph)

    if (node.modifier == Modifier.END) {
        graph.removeNode(node)
        return withGraph(graph)
    }

    val orientations: List<Pair<Float, Float>> = endPointOrientations(node)
    if (orientations.isEmpty()) return null

    val existing: Node? = endPointOf(node)
    existing?.let { graph.removeNode(it) }
    val next: Int = if (existing == null) 0 else orientations.indexOfNub(node, existing) + 1
    if (next >= orientations.size) return withGraph(graph)

    val nub: Node = node.nubAt(orientations[next])
    if (nub in graph.nodes()) return null
    graph.putEdgeValue(node, nub, Modifier.NORMAL)
    return withGraph(graph)
}

fun Panel.withGraph(graph: ValueGraph<Node, Edge>): Panel = when (this) {
    is Panel.Grid -> copy(graph = graph)
    is Panel.Tree -> copy(graph = graph)
    is Panel.Freeform -> copy(graph = graph)
}
