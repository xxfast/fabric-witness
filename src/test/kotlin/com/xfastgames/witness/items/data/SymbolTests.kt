package com.xfastgames.witness.items.data

import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.google.common.truth.Truth.assertThat
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.item.DyeColor
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Hexagons as a [Atom] held separately from a node's role and an edge's traversal state
 * (rules/witness/04-hexagon-dots.md), and the reads that keep panels saved before that split
 * loading correctly.
 */
@Suppress("UnstableApiUsage")
class SymbolTests {

    private val plain = Node(0f, 0f)
    private val dotted = Node(1f, 0f, symbol = Atom.HEXAGON)

    @Test
    fun `A node's symbol survives a round trip`() {
        val tag: CompoundTag = CompoundTag().apply { putNode(dotted) }

        assertThat(tag.getNode()).isEqualTo(dotted)
    }

    @Test
    fun `A node carries a role and a symbol at once`() {
        val start = Node(0f, 0f, Modifier.START, Atom.HEXAGON)
        val tag: CompoundTag = CompoundTag().apply { putNode(start) }

        val actual: Node = tag.getNode()
        assertThat(actual.modifier).isEqualTo(Modifier.START)
        assertThat(actual.symbol).isEqualTo(Atom.HEXAGON)
    }

    @Test
    fun `An edge's symbol survives a round trip`() {
        val graph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected()
            .build<Node, Edge>()
            .apply { putEdgeValue(plain, dotted, Edge(Modifier.NORMAL, Atom.HEXAGON)) }
        val tag: CompoundTag = CompoundTag().apply { putValueGraph(KEY_GRAPH, graph) }

        assertThat(tag.getValueGraph(KEY_GRAPH)).isEqualTo(graph)
    }

    @Test
    fun `A broken edge keeps its state and its symbol independently`() {
        val graph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected()
            .build<Node, Edge>()
            .apply { putEdgeValue(plain, dotted, Edge(Modifier.BREAK, Atom.HEXAGON)) }
        val tag: CompoundTag = CompoundTag().apply { putValueGraph(KEY_GRAPH, graph) }

        val actual: Edge = requireNotNull(tag.getValueGraph(KEY_GRAPH).edgeValue(plain, dotted).orElse(null))
        assertThat(actual.modifier).isEqualTo(Modifier.BREAK)
        assertThat(actual.symbol).isEqualTo(Atom.HEXAGON)
    }

    @Test
    fun `Growing a grid carries a node's symbol across`() {
        val source: Panel.Grid = gridOf(Node(0.5f, 0.5f, Modifier.START, Atom.HEXAGON))

        val expanded: Panel.Grid = source.expandTo(3, 3)

        val carried: Node = expanded.graph.nodes().single { it.symbol == Atom.HEXAGON }
        assertThat(carried.modifier).isEqualTo(Modifier.START)
    }

    @Nested
    @DisplayName("Panels saved before symbols were split out")
    inner class Legacy {

        @Test
        fun `A node whose modifier was a hexagon becomes a roleless node carrying one`() {
            val tag: CompoundTag = legacyNode(x = 2f, y = 3f, modifier = Modifier.DOT)

            assertThat(tag.getNode()).isEqualTo(Node(2f, 3f, Modifier.NONE, Atom.HEXAGON))
        }

        @Test
        fun `An edge whose value was a hexagon becomes a traversable edge carrying one`() {
            val tag: CompoundTag = legacyGraph(Modifier.DOT)

            val actual: Edge = requireNotNull(
                tag.getValueGraph(KEY_GRAPH).edgeValue(plain, Node(1f, 0f)).orElse(null)
            )
            assertThat(actual).isEqualTo(Edge(Modifier.NORMAL, Atom.HEXAGON))
        }

        @Test
        fun `A graph with no edge symbol array keeps every edge`() {
            // The array is absent on every panel written before this change. Defaulting it per
            // array rather than per cell would drop the whole adjacency matrix.
            val tag: CompoundTag = legacyGraph(Modifier.NORMAL)

            val actual: ValueGraph<Node, Edge> = tag.getValueGraph(KEY_GRAPH)
            assertThat(actual.edges()).hasSize(1)
            assertThat(actual.edgeValue(plain, Node(1f, 0f)).orElse(null)).isEqualTo(Edge.NORMAL)
        }

        @Test
        fun `An unknown modifier ordinal reads as none instead of throwing`() {
            // Modifier is append-only precisely so this never happens, but an out of range ordinal
            // must not take the whole world load down with it.
            val tag: CompoundTag = CompoundTag().apply {
                putFloat("x", 0f)
                putFloat("y", 0f)
                putInt("modifier", 99)
            }

            assertThat(tag.getNode()).isEqualTo(Node(0f, 0f, Modifier.NONE, Atom.NONE))
        }
    }

    @Nested
    @DisplayName("Authoring a hexagon onto a panel")
    inner class Authoring {

        private val a = Node(0.5f, 0.5f, Modifier.START)
        private val b = Node(1.5f, 0.5f)

        private fun panelOf(edge: Edge): Panel {
            val graph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected().build()
            graph.putEdgeValue(a, b, edge)
            return Panel.Freeform(
                line = com.xfastgames.witness.utils.guava.emptyGraph(),
                graph = graph,
                backgroundColor = DyeColor.WHITE,
                width = 2,
                height = 2
            )
        }

        @Test
        fun `Toggling a node on and off leaves the panel as it was`() {
            val panel: Panel = panelOf(Edge.NORMAL)

            val marked: Panel = requireNotNull(panel.withSymbolToggled(a, null))
            val cleared: Panel = requireNotNull(marked.withSymbolToggled(marked.nodeAt(a), null))

            assertThat(marked.graph.nodes().single { it.symbol == Atom.HEXAGON }.modifier)
                .isEqualTo(Modifier.START)
            assertThat(cleared.graph.nodes().none { it.symbol == Atom.HEXAGON }).isTrue()
        }

        @Test
        fun `Marking a node keeps its role and its edges`() {
            val panel: Panel = panelOf(Edge.NORMAL)

            val marked: Panel = requireNotNull(panel.withSymbolToggled(a, null))

            val start: Node = marked.graph.nodes().single { it.modifier == Modifier.START }
            assertThat(start.symbol).isEqualTo(Atom.HEXAGON)
            assertThat(marked.graph.adjacentNodes(start)).containsExactly(b)
            assertThat(marked.graph.edgeValue(start, b).orElse(null)).isEqualTo(Edge.NORMAL)
        }

        @Test
        fun `Marking an edge keeps it traversable`() {
            val panel: Panel = panelOf(Edge.NORMAL)

            val marked: Panel = requireNotNull(panel.withSymbolToggled(null, edgeOf(panel)))

            assertThat(marked.graph.edgeValue(a, b).orElse(null))
                .isEqualTo(Edge(Modifier.NORMAL, Atom.HEXAGON))
        }

        @Test
        fun `A broken edge refuses a hexagon`() {
            // The line can never reach the middle of a gap, so the panel would be unsolvable by
            // construction (rules/witness/03-broken-edges.md).
            val panel: Panel = panelOf(Edge.BREAK)

            assertThat(panel.withSymbolToggled(null, edgeOf(panel))).isNull()
        }

        @Test
        fun `A node wins over the edges that meet it`() {
            val panel: Panel = panelOf(Edge.NORMAL)

            val marked: Panel = requireNotNull(panel.withSymbolToggled(a, edgeOf(panel)))

            // Looked up via the replaced node, not `a`: marking a node produces a different Node,
            // and Node is the graph's key, so the original is no longer in the graph at all.
            val start: Node = marked.graph.nodes().single { it.symbol == Atom.HEXAGON }
            assertThat(marked.graph.edgeValue(start, b).orElse(null)?.symbol).isEqualTo(Atom.NONE)
            assertThat(marked.graph.nodes().count { it.symbol == Atom.HEXAGON }).isEqualTo(1)
        }

        @Test
        fun `Marking a node makes it a different node`() {
            // Pins the trap the test above tripped over: Node is a data class and the graph key, so
            // every field is part of its identity. Callers holding the old node must re-look it up.
            val panel: Panel = panelOf(Edge.NORMAL)

            val marked: Panel = requireNotNull(panel.withSymbolToggled(a, null))

            assertThat(marked.graph.nodes()).doesNotContain(a)
            assertThat(marked.graph.nodes()).hasSize(panel.graph.nodes().size)
        }

        @Test
        fun `Clicking neither a node nor an edge leaves the panel alone`() {
            assertThat(panelOf(Edge.NORMAL).withSymbolToggled(null, null)).isNull()
        }

        private fun edgeOf(panel: Panel) = panel.graph.edges().single()

        private fun Panel.nodeAt(node: Node): Node =
            graph.nodes().single { it.x == node.x && it.y == node.y }
    }

    private fun legacyNode(x: Float, y: Float, modifier: Modifier): CompoundTag =
        CompoundTag().apply {
            putFloat("x", x)
            putFloat("y", y)
            putInt("modifier", modifier.ordinal)
            // No "symbol" key: the field did not exist yet.
        }

    /** Two nodes joined by one edge stored the old way: an `edges` matrix and no `edgeSymbols`. */
    private fun legacyGraph(edge: Modifier): CompoundTag = CompoundTag().apply {
        put(KEY_GRAPH, CompoundTag().apply {
            put("nodes", ListTag().apply {
                add(legacyNode(0f, 0f, Modifier.NONE))
                add(legacyNode(1f, 0f, Modifier.NONE))
            })
            putIntArray("edges", intArrayOf(0, edge.ordinal, edge.ordinal, 0))
        })
    }

    private fun gridOf(vararg nodes: Node): Panel.Grid {
        val graph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected().build()
        val base: ValueGraph<Node, Edge> = Panel.Grid.generateGrid(2, 2)
        val overrides: Map<Pair<Float, Float>, Node> = nodes.associateBy { it.x to it.y }
        fun replace(node: Node): Node = overrides[node.x to node.y] ?: node
        base.edges().forEach { side ->
            graph.putEdgeValue(
                replace(side.nodeU()),
                replace(side.nodeV()),
                base.edgeValue(side.nodeU(), side.nodeV()).get()
            )
        }
        return Panel.Grid(
            line = com.xfastgames.witness.utils.guava.emptyGraph(),
            graph = graph,
            backgroundColor = DyeColor.WHITE,
            width = 2,
            height = 2
        )
    }
}
