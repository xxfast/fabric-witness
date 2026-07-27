package com.xfastgames.witness.items.data

import com.google.common.graph.MutableValueGraph
import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.items.data.Panel.Grid.Companion.gridOffsets
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

@Suppress("UnstableApiUsage")
class PanelExpansionTests {

    /** The node at index ([x], [y]), resolved through the size's centring offsets. */
    private fun Panel.Grid.nodeAt(x: Int, y: Int): Node? {
        val (xOffset: Float, yOffset: Float) = gridOffsets(width, height)
        return graph.nodes().find {
            (it.x - xOffset).roundToInt() == x && (it.y - yOffset).roundToInt() == y
        }
    }

    private fun Panel.Grid.withModifierAt(x: Int, y: Int, modifier: Modifier): Panel.Grid {
        val node: Node = nodeAt(x, y)!!
        val replacement = node.copy(modifier = modifier)
        val graph: MutableValueGraph<Node, Edge> = com.google.common.graph.ValueGraphBuilder
            .undirected()
            .build<Node, Edge>()
        this.graph.nodes().forEach { graph.addNode(if (it == node) replacement else it) }
        this.graph.edges().forEach { edge ->
            val u: Node = edge.nodeU().let { if (it == node) replacement else it }
            val v: Node = edge.nodeV().let { if (it == node) replacement else it }
            graph.putEdgeValue(u, v, this.graph.edgeValue(edge.nodeU(), edge.nodeV()).get())
        }
        return copy(graph = graph)
    }

    @Test
    fun `expansion produces a complete grid of the requested size`() {
        val expanded: Panel.Grid = Panel.Grid.ofSize(2, 2).expandTo(4, 4)

        assertThat(expanded.width).isEqualTo(4)
        assertThat(expanded.height).isEqualTo(4)
        assertThat(expanded.graph.nodes()).hasSize(16)
        // A w×h grid has (w−1)·h + w·(h−1) edges.
        assertThat(expanded.graph.edges()).hasSize(24)
        assertThat(expanded.graph).isEqualTo(Panel.Grid.ofSize(4, 4).graph)
    }

    @Test
    fun `nodes are re-centred when the aspect ratio changes`() {
        // A 2×4 grid is centred inside a 4-wide square, so index 0 sits at x = 1.5, not 0.5.
        val expanded: Panel.Grid = Panel.Grid.ofSize(2, 2).expandTo(2, 4)

        assertThat(expanded.nodeAt(0, 0)).isEqualTo(Node(1.5f, 0.5f))
        assertThat(expanded.graph).isEqualTo(Panel.Grid.ofSize(2, 4).graph)
    }

    @Test
    fun `modifiers survive the move and land at the anchor offset`() {
        val source: Panel.Grid = Panel.Grid.ofSize(2, 2)
            .withModifierAt(0, 0, Modifier.START)
            .withModifierAt(1, 1, Modifier.END)

        val centred: Panel.Grid = source.expandTo(4, 4, offsetX = 1, offsetY = 1)

        assertThat(centred.nodeAt(1, 1)?.modifier).isEqualTo(Modifier.START)
        assertThat(centred.nodeAt(2, 2)?.modifier).isEqualTo(Modifier.END)
        assertThat(centred.nodeAt(0, 0)?.modifier).isEqualTo(Modifier.NONE)
        assertThat(centred.graph.nodes().filter { it.modifier != Modifier.NONE }).hasSize(2)
    }

    @Test
    fun `the anchor decides which sides grow`() {
        val source: Panel.Grid = Panel.Grid.ofSize(2, 2).withModifierAt(0, 0, Modifier.START)

        // Anchored low: the old content stays in the low corner and the grid grows away from it.
        assertThat(source.expandTo(3, 3, offsetX = 0, offsetY = 0).nodeAt(0, 0)?.modifier)
            .isEqualTo(Modifier.START)

        // Anchored high: the same panel is pushed up and right instead.
        assertThat(source.expandTo(3, 3, offsetX = 1, offsetY = 1).nodeAt(1, 1)?.modifier)
            .isEqualTo(Modifier.START)
    }

    @Test
    fun `edge modifiers survive, including deliberately missing edges`() {
        val source: Panel.Grid = Panel.Grid.ofSize(3, 3)
        val broken: Node = source.nodeAt(0, 0)!!
        val brokenNeighbour: Node = source.nodeAt(1, 0)!!
        val removed: Node = source.nodeAt(0, 1)!!
        val removedNeighbour: Node = source.nodeAt(1, 1)!!

        val mutable: MutableValueGraph<Node, Edge> = com.google.common.graph.ValueGraphBuilder
            .undirected()
            .build<Node, Edge>()
        source.graph.nodes().forEach(mutable::addNode)
        source.graph.edges().forEach { edge ->
            mutable.putEdgeValue(
                edge.nodeU(),
                edge.nodeV(),
                source.graph.edgeValue(edge.nodeU(), edge.nodeV()).get()
            )
        }
        mutable.putEdgeValue(broken, brokenNeighbour, Modifier.BREAK)
        mutable.removeEdge(removed, removedNeighbour)

        val expanded: Panel.Grid = source.copy(graph = mutable).expandTo(4, 4)

        assertThat(expanded.graph.edgeValue(expanded.nodeAt(0, 0)!!, expanded.nodeAt(1, 0)!!).get())
            .isEqualTo(Modifier.BREAK)
        assertThat(expanded.graph.hasEdgeConnecting(expanded.nodeAt(0, 1)!!, expanded.nodeAt(1, 1)!!))
            .isFalse()
        // Newly added cells are ordinary.
        assertThat(expanded.graph.edgeValue(expanded.nodeAt(3, 3)!!, expanded.nodeAt(3, 2)!!).get())
            .isEqualTo(Modifier.NORMAL)
    }

    @Test
    fun `the drawn line is dropped`() {
        val source: Panel.Grid = Panel.Grid.ofSize(2, 2)
        assertThat(source.expandTo(3, 3).line.nodes()).isEmpty()
    }

    @Test
    fun `shrinking is refused`() {
        val source: Panel.Grid = Panel.Grid.ofSize(4, 4)
        assertThat(source.expandTo(2, 2)).isEqualTo(source)
    }
}
