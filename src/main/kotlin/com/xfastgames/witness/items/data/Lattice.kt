@file:Suppress("UnstableApiUsage")

package com.xfastgames.witness.items.data

import com.google.common.graph.Graphs
import com.google.common.graph.MutableValueGraph
import kotlin.math.abs
import kotlin.math.hypot

/**
 * The Grid tab (rules/minecraft/04-2-puzzle-composer-grid.md): editing a panel's *topology* rather
 * than what it means.
 *
 * A panel type answers three questions and nothing else: where a node may sit, which two nodes may
 * be joined, and what its cells are (cells are out of scope here; they decide the Modifiers tab's
 * region symbols). [anchors] and [canJoin] are those first two answers. Everything else in this
 * file, adding, removing, toggling a segment, filling, clearing, is one editor built on top of
 * them, shared by every panel type that answers them.
 *
 * Absence needs no new data: a node or edge simply missing from the [Panel.graph] already means
 * what the design says everywhere downstream (renderer, solver, `expandTo`, NBT round trip). This
 * file's job is only to know *where* a node is allowed to be absent from, which the graph alone
 * cannot say.
 */

/** Tolerance for comparing panel positions, in panel units. Mirrors `EndPoints.BORDER_EPSILON`. */
const val ANCHOR_EPSILON = 0.001f

private fun near(a: Float, b: Float): Boolean = abs(a - b) <= ANCHOR_EPSILON

private fun Panel.isAnchor(x: Float, y: Float): Boolean =
    anchors().any { anchor -> near(anchor.x, x) && near(anchor.y, y) }

/**
 * Every position a node may occupy on this panel, present or not. Bare nodes: positions only,
 * carrying no modifier or symbol, since a position is not the node that may or may not be sitting
 * on it.
 *
 * Only [Panel.Grid] has a finite, drawable anchor set today. [Panel.Tree]'s anchors are the
 * branch positions of its tree and [Panel.Freeform]'s are "anywhere"; neither is a set this editor
 * can enumerate or hit-test, so both answer with nothing rather than a wrong answer
 * (rules/minecraft/04-2-puzzle-composer-grid.md#what-it-does-not-do).
 */
fun Panel.anchors(): List<Node> = when (this) {
    is Panel.Grid -> {
        val (xOffset: Float, yOffset: Float) = Panel.Grid.gridOffsets(width, height)
        buildList {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    add(Node(x + xOffset, y + yOffset))
                }
            }
        }
    }

    is Panel.Tree, is Panel.Freeform -> emptyList()
}

/**
 * Whether these two positions may be joined by a segment on this panel type.
 *
 * Compares position only, never node equality: [a] and [b] may carry whatever modifier or symbol
 * the actual node on the graph has, and none of that changes whether the pair is adjacent.
 *
 * A grid pair joins when both positions are anchors and differ by exactly one unit on a single
 * axis, i.e. a row or column neighbour. [Panel.Tree] and [Panel.Freeform] are not editable in this
 * MVP, so nothing on them may be joined here.
 */
fun Panel.canJoin(a: Node, b: Node): Boolean {
    if (this !is Panel.Grid) return false
    if (!isAnchor(a.x, a.y) || !isAnchor(b.x, b.y)) return false

    val dx: Float = abs(a.x - b.x)
    val dy: Float = abs(a.y - b.y)
    return (near(dx, 1f) && near(dy, 0f)) || (near(dy, 1f) && near(dx, 0f))
}

/** The node actually present at this position, if any. */
fun Panel.nodeAt(x: Float, y: Float): Node? =
    graph.nodes().find { node -> near(node.x, x) && near(node.y, y) }

/**
 * The joinable anchor pair nearest to ([x], [y]), if the position is within [tolerance] panel units
 * of the segment between them.
 *
 * Hit-tests the *lattice*, not [Panel.graph]: the pencil has to be able to click a segment that is
 * not there yet, so a pair counts whether or not anything is currently drawn between them. Callers
 * check for an anchor first, since a position near a node is near all the segments meeting it.
 */
fun Panel.nearestJoinablePair(x: Float, y: Float, tolerance: Float): Pair<Node, Node>? {
    var best: Pair<Node, Node>? = null
    var bestDistance: Float = Float.MAX_VALUE

    anchors().forEach { anchor ->
        // Only the neighbours one step along each axis, so every pair is considered once.
        listOf(Node(anchor.x + 1f, anchor.y), Node(anchor.x, anchor.y + 1f)).forEach { neighbour ->
            if (canJoin(anchor, neighbour)) {
                val distance: Float = distanceToSegment(x, y, anchor, neighbour)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = anchor to neighbour
                }
            }
        }
    }

    return best?.takeIf { bestDistance <= tolerance }
}

/** Perpendicular distance from ([x], [y]) to the segment [a]-[b], clamped to the segment's ends. */
private fun distanceToSegment(x: Float, y: Float, a: Node, b: Node): Float {
    val dx: Float = b.x - a.x
    val dy: Float = b.y - a.y
    val lengthSquared: Float = dx * dx + dy * dy
    if (lengthSquared <= ANCHOR_EPSILON) return hypot(x - a.x, y - a.y)

    val along: Float = (((x - a.x) * dx + (y - a.y) * dy) / lengthSquared).coerceIn(0f, 1f)
    return hypot(x - (a.x + along * dx), y - (a.y + along * dy))
}

/**
 * The anchors a drag crosses on its way from [from] to [to], in order, excluding [from] itself.
 *
 * A drag reports a cursor position per frame, so a fast sweep jumps several anchors at once. Rather
 * than drop those steps and leave gaps in a stroke, the path is walked one unit at a time, x axis
 * first, then y. A diagonal sweep therefore lays down a staircase, which is the only thing a
 * grid-constrained stroke can mean.
 *
 * Empty when either end is off the lattice, or on a type with no anchors.
 */
fun Panel.anchorPathBetween(from: Node, to: Node): List<Node> {
    if (!isAnchor(from.x, from.y) || !isAnchor(to.x, to.y)) return emptyList()

    val steps: MutableList<Node> = mutableListOf()
    var x: Float = from.x
    var y: Float = from.y
    // Anchors sit one unit apart, so a walk can never need more steps than there are anchors.
    val limit: Int = anchors().size + 1
    while (steps.size < limit) {
        val dx: Float = to.x - x
        val dy: Float = to.y - y
        if (abs(dx) <= ANCHOR_EPSILON && abs(dy) <= ANCHOR_EPSILON) break

        if (abs(dx) > ANCHOR_EPSILON) x += if (dx > 0f) 1f else -1f
        else y += if (dy > 0f) 1f else -1f
        steps += Node(x, y)
    }
    return steps
}

/**
 * Adds a node at this anchor, joined by [Edge.NORMAL] to every neighbour already present. No-op if
 * the anchor is already occupied, or if [x]/[y] is not an anchor at all (including on a [Panel.Tree]
 * or [Panel.Freeform], which have none).
 */
fun Panel.withNodeAdded(x: Float, y: Float): Panel {
    if (!isAnchor(x, y)) return this
    if (nodeAt(x, y) != null) return this

    val newNode = Node(x, y)
    val updated: MutableValueGraph<Node, Edge> = Graphs.copyOf(graph)
    updated.addNode(newNode)

    anchors()
        .filter { anchor -> canJoin(newNode, anchor) }
        .mapNotNull { anchor -> nodeAt(anchor.x, anchor.y) }
        .forEach { neighbour -> updated.putEdgeValue(newNode, neighbour, Edge.NORMAL) }

    return withGraph(updated)
}

/**
 * Removes [node], its segments, and any [Modifier.END] nub hanging off it. A nub is a node plus
 * the one edge holding it on (`EndPoints.kt`), so it cannot outlive the node it hangs from
 * (rules/minecraft/04-2-puzzle-composer-grid.md#edge-cases). No-op if [node] is not present.
 */
fun Panel.withNodeRemoved(node: Node): Panel {
    if (node !in graph.nodes()) return this

    val updated: MutableValueGraph<Node, Edge> = Graphs.copyOf(graph)
    val nub: Node? = updated.adjacentNodes(node).firstOrNull { it.modifier == Modifier.END }
    nub?.let { updated.removeNode(it) }
    updated.removeNode(node)
    return withGraph(updated)
}

/**
 * The pencil's stroke: joins the anchors at ([ax], [ay]) and ([bx], [by]) with a plain
 * [Edge.NORMAL] segment, laying down either endpoint if the stroke crosses an anchor that is empty.
 *
 * Unlike [withNodeAdded], a node laid down by a stroke is **not** joined to its other present
 * neighbours. A stroke leaves exactly what was drawn, so dragging a line alongside existing
 * geometry does not weld the two together
 * (rules/minecraft/04-2-puzzle-composer-grid.md#pencil-and-eraser).
 *
 * Null when the type says this pair may not be joined, or the segment is already there, so a drag
 * that re-crosses its own stroke costs nothing.
 */
fun Panel.withSegmentAdded(ax: Float, ay: Float, bx: Float, by: Float): Panel? {
    val a = Node(ax, ay)
    val b = Node(bx, by)
    if (!canJoin(a, b)) return null

    val updated: MutableValueGraph<Node, Edge> = Graphs.copyOf(graph)
    val nodeA: Node = nodeAt(ax, ay) ?: a.also { updated.addNode(it) }
    val nodeB: Node = nodeAt(bx, by) ?: b.also { updated.addNode(it) }
    if (updated.hasEdgeConnecting(nodeA, nodeB)) return null

    updated.putEdgeValue(nodeA, nodeB, Edge.NORMAL)
    return withGraph(updated)
}

/**
 * The eraser's stroke: removes the segment between the anchors at ([ax], [ay]) and ([bx], [by]),
 * whatever its modifier, an [Edge.BREAK] included, since deleting is the stronger of the two
 * (rules/minecraft/04-2-puzzle-composer-grid.md#edge-cases).
 *
 * An endpoint left joined to nothing goes with it. A node with no segments is inert everywhere
 * downstream, invisible on a finished panel and unreachable by any line, so an eraser that left one
 * behind would be depositing residue rather than erasing. Placing one deliberately is still the
 * pencil's business. Whichever endpoint keeps a segment stays exactly as it was, marks included,
 * which is what makes erasing a single segment the way to author a deliberate gap.
 *
 * Null when there is nothing to erase.
 */
fun Panel.withSegmentRemoved(ax: Float, ay: Float, bx: Float, by: Float): Panel? {
    if (!canJoin(Node(ax, ay), Node(bx, by))) return null
    val nodeA: Node = nodeAt(ax, ay) ?: return null
    val nodeB: Node = nodeAt(bx, by) ?: return null

    val updated: MutableValueGraph<Node, Edge> = Graphs.copyOf(graph)
    if (!updated.hasEdgeConnecting(nodeA, nodeB)) return null

    updated.removeEdge(nodeA, nodeB)
    // Only these two can have been left bare: nothing else lost an edge. A nub counts as a segment
    // here, so a border node keeps its end point rather than being swept up with it.
    listOf(nodeA, nodeB)
        .filter { node -> updated.adjacentNodes(node).isEmpty() }
        .forEach { node -> updated.removeNode(node) }
    return withGraph(updated)
}
