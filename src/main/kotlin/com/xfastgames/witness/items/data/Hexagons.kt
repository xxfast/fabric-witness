@file:Suppress("UnstableApiUsage")

package com.xfastgames.witness.items.data

/**
 * The hexagon rule (rules/witness/04-hexagon-dots.md): the path must cover every hexagon on the
 * panel. A hexagon on a node is covered when the path visits that node; a hexagon on an edge when
 * the path traverses that edge, entering at one endpoint and leaving at the other.
 *
 * Checked against the path itself, never against a region, so this needs no flood fill and is
 * O(hexagons) once the path is known.
 */
sealed class Hexagon {
    data class OnNode(val node: Node) : Hexagon()
    data class OnEdge(val u: Node, val v: Node) : Hexagon()
}

/** Every hexagon on this panel, in no particular order. OrderPolicy never matters to the rule. */
fun Panel.hexagons(): List<Hexagon> {
    val nodes: List<Hexagon> = graph.nodes()
        .filter { node -> node.symbol == Atom.HEXAGON }
        .map { node -> Hexagon.OnNode(node) }
    val edges: List<Hexagon> = graph.edges()
        .filter { side -> graph.edgeValue(side.nodeU(), side.nodeV()).orElse(null)?.symbol == Atom.HEXAGON }
        .map { side -> Hexagon.OnEdge(side.nodeU(), side.nodeV()) }
    return nodes + edges
}

/**
 * The hexagons [path] fails to cover, empty when the panel's hexagons are all satisfied. Returns the
 * failures rather than a verdict so a caller can eventually point at the ones that were missed.
 *
 * [path] is the submitted line as an ordered node list, so consecutive entries are the edges it
 * actually drew. Direction is irrelevant: an edge counts whichever way it was crossed.
 */
fun Panel.unsatisfiedHexagons(path: List<Node>): List<Hexagon> {
    if (path.isEmpty()) return hexagons()
    val visited: Set<Node> = path.toSet()
    val traversed: Set<Set<Node>> = path.zipWithNext().map { (from, to) -> setOf(from, to) }.toSet()

    return hexagons().filterNot { hexagon ->
        when (hexagon) {
            is Hexagon.OnNode -> hexagon.node in visited
            is Hexagon.OnEdge -> setOf(hexagon.u, hexagon.v) in traversed
        }
    }
}
