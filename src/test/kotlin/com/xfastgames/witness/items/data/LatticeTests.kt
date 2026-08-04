package com.xfastgames.witness.items.data

import com.google.common.graph.Graphs
import com.google.common.graph.MutableValueGraph
import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.utils.guava.mutableValueGraph
import net.minecraft.world.item.DyeColor
import org.junit.jupiter.api.Test

@Suppress("UnstableApiUsage")
class LatticeTests {

    private val panel: Panel.Grid = Panel.Grid.ofSize(3)

    @Test
    fun `a fresh 3x3 grid has 9 anchors, all occupied`() {
        val anchors: List<Node> = panel.anchors()
        assertThat(anchors).hasSize(9)
        anchors.forEach { anchor -> assertThat(panel.nodeAt(anchor.x, anchor.y)).isNotNull() }
    }

    @Test
    fun `deleting a node removes it and its segments, leaving the others alone`() {
        val centre: Node = requireNotNull(panel.nodeAt(1.5f, 1.5f))
        val neighbours: Set<Node> = panel.graph.adjacentNodes(centre)
        assertThat(neighbours).hasSize(4)

        val updated: Panel = panel.withNodeRemoved(centre)

        assertThat(updated.graph.nodes()).doesNotContain(centre)
        assertThat(updated.graph.nodes()).hasSize(panel.graph.nodes().size - 1)
        neighbours.forEach { neighbour ->
            assertThat(updated.graph.nodes()).contains(neighbour)
            assertThat(updated.graph.adjacentNodes(neighbour)).doesNotContain(centre)
        }
    }

    @Test
    fun `deleting a node also removes the end point nub hanging off it`() {
        val borderNode: Node = requireNotNull(panel.nodeAt(1.5f, 0.5f))
        val withNub: Panel = requireNotNull(panel.withEndPointToggled(borderNode))
        val nub: Node = requireNotNull(withNub.endPointOf(borderNode))

        val updated: Panel = withNub.withNodeRemoved(borderNode)

        assertThat(updated.graph.nodes()).doesNotContain(borderNode)
        assertThat(updated.graph.nodes()).doesNotContain(nub)
    }

    @Test
    fun `adding a node back rejoins it to its present neighbours`() {
        val centre: Node = requireNotNull(panel.nodeAt(1.5f, 1.5f))
        val neighbours: Set<Node> = panel.graph.adjacentNodes(centre)

        val carved: Panel = panel.withNodeRemoved(centre)
        val restored: Panel = carved.withNodeAdded(1.5f, 1.5f)

        val restoredNode: Node = requireNotNull(restored.nodeAt(1.5f, 1.5f))
        assertThat(restoredNode).isEqualTo(Node(1.5f, 1.5f))
        assertThat(restored.graph.adjacentNodes(restoredNode)).containsExactlyElementsIn(neighbours)
    }

    @Test
    fun `canJoin is true for orthogonal neighbours, false for diagonals, two-apart pairs and nubs`() {
        val bottomLeft = Node(0.5f, 0.5f)
        val bottomMiddle = Node(1.5f, 0.5f)
        val middle = Node(1.5f, 1.5f)
        val bottomRight = Node(2.5f, 0.5f)

        assertThat(panel.canJoin(bottomLeft, bottomMiddle)).isTrue()
        assertThat(panel.canJoin(bottomLeft, middle)).isFalse()
        assertThat(panel.canJoin(bottomLeft, bottomRight)).isFalse()

        val withNub: Panel = requireNotNull(panel.withEndPointToggled(bottomMiddle))
        val nub: Node = requireNotNull(withNub.endPointOf(bottomMiddle))
        assertThat(withNub.canJoin(bottomMiddle, nub)).isFalse()
    }

    @Test
    fun `the eraser lifts a segment, keeping endpoints that still have one`() {
        val a: Node = requireNotNull(panel.nodeAt(0.5f, 0.5f))
        val b: Node = requireNotNull(panel.nodeAt(1.5f, 0.5f))
        assertThat(panel.graph.hasEdgeConnecting(a, b)).isTrue()

        val erased: Panel = requireNotNull(panel.withSegmentRemoved(0.5f, 0.5f, 1.5f, 0.5f))

        assertThat(erased.graph.hasEdgeConnecting(a, b)).isFalse()
        // Both corners keep their other segment up the column, so neither is swept up.
        assertThat(erased.graph.nodes()).containsAtLeast(a, b)
    }

    @Test
    fun `erasing a node's last segment takes the node with it`() {
        // Carve the bottom left corner down to a single segment along the bottom row.
        val carved: Panel = requireNotNull(panel.withSegmentRemoved(0.5f, 0.5f, 0.5f, 1.5f))
        val corner: Node = requireNotNull(carved.nodeAt(0.5f, 0.5f))
        assertThat(carved.graph.adjacentNodes(corner)).hasSize(1)

        val erased: Panel = requireNotNull(carved.withSegmentRemoved(0.5f, 0.5f, 1.5f, 0.5f))

        assertThat(erased.nodeAt(0.5f, 0.5f)).isNull()
        // The far end still has segments of its own, so it stays.
        assertThat(erased.nodeAt(1.5f, 0.5f)).isNotNull()
    }

    @Test
    fun `erasing the last segment of a lone pair takes both nodes`() {
        val empty: Panel = panel.withGraph(mutableValueGraph())
        val pair: Panel = requireNotNull(empty.withSegmentAdded(0.5f, 0.5f, 1.5f, 0.5f))
        assertThat(pair.graph.nodes()).hasSize(2)

        val erased: Panel = requireNotNull(pair.withSegmentRemoved(0.5f, 0.5f, 1.5f, 0.5f))

        assertThat(erased.graph.nodes()).isEmpty()
    }

    @Test
    fun `a border node keeps its end point when its last grid segment goes`() {
        val border: Node = requireNotNull(panel.nodeAt(1.5f, 0.5f))
        val withNub: Panel = requireNotNull(panel.withEndPointToggled(border))

        // Strip every grid segment on that border node; the nub is the only thing left holding it.
        val stripped: Panel = withNub
            .let { requireNotNull(it.withSegmentRemoved(1.5f, 0.5f, 0.5f, 0.5f)) }
            .let { requireNotNull(it.withSegmentRemoved(1.5f, 0.5f, 2.5f, 0.5f)) }
            .let { requireNotNull(it.withSegmentRemoved(1.5f, 0.5f, 1.5f, 1.5f)) }

        val kept: Node = requireNotNull(stripped.nodeAt(1.5f, 0.5f))
        assertThat(stripped.endPointOf(kept)).isNotNull()
    }

    @Test
    fun `the eraser lifts a broken segment too, since deleting is the stronger of the two`() {
        val a: Node = requireNotNull(panel.nodeAt(0.5f, 0.5f))
        val b: Node = requireNotNull(panel.nodeAt(1.5f, 0.5f))
        val brokenGraph: MutableValueGraph<Node, Edge> = Graphs.copyOf(panel.graph)
        brokenGraph.putEdgeValue(a, b, Edge.BREAK)

        val erased: Panel =
            requireNotNull(panel.withGraph(brokenGraph).withSegmentRemoved(0.5f, 0.5f, 1.5f, 0.5f))

        assertThat(erased.graph.hasEdgeConnecting(a, b)).isFalse()
    }

    @Test
    fun `the eraser reports nothing to do when the segment is already gone`() {
        val erased: Panel = requireNotNull(panel.withSegmentRemoved(0.5f, 0.5f, 1.5f, 0.5f))
        assertThat(erased.withSegmentRemoved(0.5f, 0.5f, 1.5f, 0.5f)).isNull()
    }

    @Test
    fun `the pencil lays down both endpoints of a stroke over empty anchors`() {
        val cleared: Panel = panel
            .withNodeRemoved(requireNotNull(panel.nodeAt(0.5f, 0.5f)))
            .let { carved -> carved.withNodeRemoved(requireNotNull(carved.nodeAt(1.5f, 0.5f))) }
        assertThat(cleared.nodeAt(0.5f, 0.5f)).isNull()

        val drawn: Panel = requireNotNull(cleared.withSegmentAdded(0.5f, 0.5f, 1.5f, 0.5f))

        val a: Node = requireNotNull(drawn.nodeAt(0.5f, 0.5f))
        val b: Node = requireNotNull(drawn.nodeAt(1.5f, 0.5f))
        assertThat(drawn.graph.edgeValue(a, b).get()).isEqualTo(Edge.NORMAL)
    }

    @Test
    fun `a stroke leaves exactly what was drawn, without welding to its other neighbours`() {
        val corner: Node = requireNotNull(panel.nodeAt(0.5f, 0.5f))
        val carved: Panel = panel.withNodeRemoved(corner)

        // Redraw the corner as a stroke along the bottom row. The node above it is still present,
        // but a stroke must not join to it the way withNodeAdded would.
        val drawn: Panel = requireNotNull(carved.withSegmentAdded(0.5f, 0.5f, 1.5f, 0.5f))

        val redrawn: Node = requireNotNull(drawn.nodeAt(0.5f, 0.5f))
        val above: Node = requireNotNull(drawn.nodeAt(0.5f, 1.5f))
        assertThat(drawn.graph.adjacentNodes(redrawn)).doesNotContain(above)
        assertThat(drawn.graph.adjacentNodes(redrawn)).hasSize(1)
    }

    @Test
    fun `the pencil reports nothing to do when the segment is already there`() {
        assertThat(panel.withSegmentAdded(0.5f, 0.5f, 1.5f, 0.5f)).isNull()
    }

    @Test
    fun `neither stroke joins a pair the type refuses`() {
        assertThat(panel.withSegmentAdded(0.5f, 0.5f, 1.5f, 1.5f)).isNull()
        assertThat(panel.withSegmentRemoved(0.5f, 0.5f, 2.5f, 0.5f)).isNull()
    }

    @Test
    fun `a click between two anchors resolves to the segment joining them`() {
        // Midway along the bottom row's left segment.
        val pair: Pair<Node, Node> = requireNotNull(panel.nearestJoinablePair(1.0f, 0.5f, 0.2f))
        assertThat(pair.toList()).containsExactly(Node(0.5f, 0.5f), Node(1.5f, 0.5f))
    }

    @Test
    fun `a click in the middle of a cell resolves to no segment`() {
        assertThat(panel.nearestJoinablePair(1.0f, 1.0f, 0.2f)).isNull()
    }

    @Test
    fun `segment hit testing finds pairs the panel has not drawn yet`() {
        val erased: Panel = requireNotNull(panel.withSegmentRemoved(0.5f, 0.5f, 1.5f, 0.5f))
        val pair: Pair<Node, Node> = requireNotNull(erased.nearestJoinablePair(1.0f, 0.5f, 0.2f))
        assertThat(pair.toList()).containsExactly(Node(0.5f, 0.5f), Node(1.5f, 0.5f))
    }

    @Test
    fun `a fast drag walks the lattice one unit at a time rather than leaving a gap`() {
        val path: List<Node> = panel.anchorPathBetween(Node(0.5f, 0.5f), Node(2.5f, 0.5f))
        assertThat(path).containsExactly(Node(1.5f, 0.5f), Node(2.5f, 0.5f)).inOrder()
    }

    @Test
    fun `a diagonal drag becomes a staircase, x axis first`() {
        val path: List<Node> = panel.anchorPathBetween(Node(0.5f, 0.5f), Node(1.5f, 1.5f))
        assertThat(path).containsExactly(Node(1.5f, 0.5f), Node(1.5f, 1.5f)).inOrder()
    }

    @Test
    fun `a drag that has not left its anchor walks nowhere`() {
        assertThat(panel.anchorPathBetween(Node(0.5f, 0.5f), Node(0.5f, 0.5f))).isEmpty()
    }

    @Test
    fun `carving survives an NBT round trip`() {
        val coloured: Panel.Grid = Panel.Grid.ofSize(3).copy(backgroundColor = DyeColor.RED)
        val carved: Panel = coloured
            .withNodeRemoved(requireNotNull(coloured.nodeAt(2.5f, 2.5f)))
            .let { panel -> requireNotNull(panel.withSegmentRemoved(0.5f, 0.5f, 1.5f, 0.5f)) }

        val restored: Panel = carved.toNbt().toPanel()

        assertThat(restored.graph.nodes()).hasSize(8)
        assertThat(restored.nodeAt(2.5f, 2.5f)).isNull()
        val a: Node = requireNotNull(restored.nodeAt(0.5f, 0.5f))
        val b: Node = requireNotNull(restored.nodeAt(1.5f, 0.5f))
        assertThat(restored.graph.hasEdgeConnecting(a, b)).isFalse()
        assertThat(restored.backgroundColor).isEqualTo(DyeColor.RED)
    }
}
