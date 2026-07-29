package com.xfastgames.witness.items.data

import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.utils.guava.emptyGraph
import net.minecraft.util.DyeColor
import org.junit.jupiter.api.Test

/**
 * The hexagon rule (rules/witness/04-hexagon-dots.md): the path must cover every hexagon, on nodes
 * and on edges alike.
 *
 * The panel is a 2x2 square of nodes, so there are two ways round from [start] to [finish]:
 *
 * ```
 *   topLeft ---- topRight
 *      |            |
 *    start ------ corner
 * ```
 */
class HexagonTests {

    private val start = Node(0f, 0f, Modifier.START)
    private val corner = Node(1f, 0f)
    private val topRight = Node(1f, 1f, Modifier.END)
    private val topLeft = Node(0f, 1f)

    /**
     * Paths are resolved against the panel's own nodes, never the bare fixtures above: marking a
     * node produces a different [Node], and the solver's path always holds the graph's copies.
     */
    private fun Panel.shortWay(): List<Node> = pathOf(start, corner, topRight)

    /** The long way round, via topLeft. */
    private fun Panel.longWay(): List<Node> = pathOf(start, topLeft, topRight)

    private fun Panel.pathOf(vararg nodes: Node): List<Node> = nodes.map { node ->
        graph.nodes().single { it.x == node.x && it.y == node.y }
    }

    @Test
    fun `A panel with no hexagons is satisfied by any path`() {
        assertThat(panelOf().let { it.unsatisfiedHexagons(it.shortWay()) }).isEmpty()
    }

    @Test
    fun `A node hexagon the path visits is satisfied`() {
        val panel: Panel = panelOf(nodeHexagons = setOf(corner))

        assertThat(panel.unsatisfiedHexagons(panel.shortWay())).isEmpty()
    }

    @Test
    fun `A node hexagon the path misses fails`() {
        val panel: Panel = panelOf(nodeHexagons = setOf(topLeft))

        assertThat(panel.unsatisfiedHexagons(panel.shortWay()))
            .containsExactly(Hexagon.OnNode(topLeft.copy(symbol = Symbol.HEXAGON)))
    }

    @Test
    fun `An edge hexagon the path traverses is satisfied`() {
        val panel: Panel = panelOf(edgeHexagons = setOf(start to corner))

        assertThat(panel.unsatisfiedHexagons(panel.shortWay())).isEmpty()
    }

    @Test
    fun `An edge hexagon the path misses fails`() {
        val panel: Panel = panelOf(edgeHexagons = setOf(start to topLeft))

        assertThat(panel.unsatisfiedHexagons(panel.shortWay())).hasSize(1)
    }

    @Test
    fun `An edge counts whichever direction it was crossed`() {
        val panel: Panel = panelOf(edgeHexagons = setOf(corner to start))

        // The edge was authored corner-to-start; the path walks it start-to-corner.
        assertThat(panel.unsatisfiedHexagons(panel.shortWay())).isEmpty()
    }

    @Test
    fun `Visiting both endpoints without using the edge does not satisfy an edge hexagon`() {
        // start and topRight are both on the path, but the edge between them is never drawn: the
        // path goes the long way round through topLeft.
        val panel: Panel = panelOf(edgeHexagons = setOf(start to corner))

        assertThat(panel.unsatisfiedHexagons(panel.longWay())).hasSize(1)
    }

    @Test
    fun `Order of crossing does not matter`() {
        val panel: Panel = panelOf(nodeHexagons = setOf(corner, topRight))

        assertThat(panel.unsatisfiedHexagons(panel.shortWay())).isEmpty()
        assertThat(panel.unsatisfiedHexagons(panel.shortWay().reversed())).isEmpty()
    }

    @Test
    fun `Every hexagon must be covered, not just one`() {
        val panel: Panel = panelOf(nodeHexagons = setOf(corner, topLeft))

        assertThat(panel.unsatisfiedHexagons(panel.shortWay())).hasSize(1)
    }

    @Test
    fun `An empty path satisfies nothing`() {
        val panel: Panel = panelOf(nodeHexagons = setOf(corner))

        assertThat(panel.unsatisfiedHexagons(emptyList())).hasSize(1)
    }

    /**
     * A 2x2 square of nodes, with [nodeHexagons] marked on nodes and [edgeHexagons] on edges.
     * Marking a node makes a different [Node], so the marked copies are substituted everywhere.
     */
    @Suppress("UnstableApiUsage")
    private fun panelOf(
        nodeHexagons: Set<Node> = emptySet(),
        edgeHexagons: Set<Pair<Node, Node>> = emptySet()
    ): Panel {
        fun mark(node: Node): Node =
            if (node in nodeHexagons) node.copy(symbol = Symbol.HEXAGON) else node

        fun edgeFor(u: Node, v: Node): Edge {
            val dotted: Boolean = edgeHexagons.any { (a, b) ->
                (a == u && b == v) || (a == v && b == u)
            }
            return Edge(Modifier.NORMAL, if (dotted) Symbol.HEXAGON else Symbol.NONE)
        }

        val graph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected().build()
        listOf(
            start to corner,
            corner to topRight,
            topRight to topLeft,
            topLeft to start
        ).forEach { (u, v) -> graph.putEdgeValue(mark(u), mark(v), edgeFor(u, v)) }

        return Panel.Grid(
            line = emptyGraph(),
            graph = graph,
            backgroundColor = DyeColor.WHITE,
            width = 1,
            height = 1
        )
    }
}
