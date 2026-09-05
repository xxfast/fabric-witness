package com.xfastgames.witness.items.data

import com.google.common.graph.EndpointPair
import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.utils.guava.emptyGraph
import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

const val KEY_GRAPH = "testGraph"

@Suppress("UnstableApiUsage")
class GraphTests {

    private val bottomRight = Node(0f, 0f, Modifier.START)
    private val bottomLeft = Node(1f, 0f)
    private val topRight = Node(0f, 1f)
    private val topLeft = Node(1f, 1f, Modifier.END)

    private val testGraph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected()
        .build<Node, Edge>().apply {
            putEdgeValue(bottomRight, topRight, Edge.NORMAL)
            putEdgeValue(topRight, topLeft, Edge.NORMAL)
            putEdgeValue(topLeft, bottomLeft, Edge.NORMAL)
            putEdgeValue(bottomLeft, bottomRight, Edge.NORMAL)
        }

    @Test
    fun `Test put and read grid`() {
        val tag: CompoundTag = CompoundTag().apply { putValueGraph(KEY_GRAPH, testGraph) }
        println(tag)
        val actual: ValueGraph<Node, Edge> = tag.getValueGraph(KEY_GRAPH)
        assertThat(actual).isEqualTo(testGraph)
    }

    @Nested
    @DisplayName("Test grid generation")
    inner class TestGridGeneration {

        @Test
        fun `Test generate grid 2x2`() {
            val actual: ValueGraph<Node, Edge> = Panel.Grid.generateGrid(2, 2)
            val expected: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected()
                .build<Node, Edge>().apply {
                    val bottomLeft = Node(0.5f, 0.5f)
                    val bottomRight = Node(0.5f, 1.5f)
                    val topLeft = Node(1.5f, 0.5f)
                    val topRight = Node(1.5f, 1.5f)

                    putEdgeValue(bottomLeft, bottomRight, Edge.NORMAL)
                    putEdgeValue(topLeft, bottomLeft, Edge.NORMAL)
                    putEdgeValue(topRight, topLeft, Edge.NORMAL)
                    putEdgeValue(topRight, bottomRight, Edge.NORMAL)
                }

            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `Test generate grid 3x3`() {
            val actual: ValueGraph<Node, Edge> = Panel.Grid.generateGrid(3, 3)
            val expected: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected()
                .build<Node, Edge>().apply {
                    val bottomLeft = Node(0.5f, 0.5f)
                    val bottomMiddle = Node(0.5f, 1.5f)
                    val bottomRight = Node(0.5f, 2.5f)

                    val middleLeft = Node(1.5f, 0.5f)
                    val middleMiddle = Node(1.5f, 1.5f)
                    val middleRight = Node(1.5f, 2.5f)

                    val topLeft = Node(2.5f, 0.5f)
                    val topMiddle = Node(2.5f, 1.5f)
                    val topRight = Node(2.5f, 2.5f)

                    putEdgeValue(bottomLeft, bottomMiddle, Edge.NORMAL)
                    putEdgeValue(bottomMiddle, bottomRight, Edge.NORMAL)
                    putEdgeValue(middleLeft, middleMiddle, Edge.NORMAL)
                    putEdgeValue(middleMiddle, middleRight, Edge.NORMAL)
                    putEdgeValue(topLeft, topMiddle, Edge.NORMAL)
                    putEdgeValue(topMiddle, topRight, Edge.NORMAL)
                    putEdgeValue(topLeft, middleLeft, Edge.NORMAL)
                    putEdgeValue(middleLeft, bottomLeft, Edge.NORMAL)
                    putEdgeValue(topMiddle, middleMiddle, Edge.NORMAL)
                    putEdgeValue(middleMiddle, bottomMiddle, Edge.NORMAL)
                    putEdgeValue(topRight, middleRight, Edge.NORMAL)
                    putEdgeValue(middleRight, bottomRight, Edge.NORMAL)
                }

            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("Test tree generation")
    inner class TestTreeGeneration {

        /** [graph] with [old] replaced by [new], keeping every edge it had. */
        private fun ValueGraph<Node, Edge>.withNode(old: Node, new: Node): MutableValueGraph<Node, Edge> {
            val copy: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected().build()
            nodes().forEach { node -> copy.addNode(if (node == old) new else node) }
            edges().forEach { pair ->
                val u: Node = if (pair.nodeU() == old) new else pair.nodeU()
                val v: Node = if (pair.nodeV() == old) new else pair.nodeV()
                copy.putEdgeValue(u, v, edgeValue(pair.nodeU(), pair.nodeV()).get())
            }
            return copy
        }

        private fun ValueGraph<Node, Edge>.branchNodes(): List<Node> = nodes().filter { it.modifier != Modifier.END }

        private fun ValueGraph<Node, Edge>.tips(): List<Node> = with(Panel.Tree) { tips() }

        private fun ValueGraph<Node, Edge>.root(): Node = branchNodes().minBy { it.y }

        private fun ValueGraph<Node, Edge>.nubs(): List<Node> = nodes().filter { it.modifier == Modifier.END }

        @Test
        fun `Test generate tree 1 tall`() {
            // The layout in rules/minecraft/01-1-tree-panel.md#layout on the 3-unit minimum panel:
            // crown 77% of the panel wide, tree 65% tall and centred, trunk 0.75 of the first level.
            val actual: ValueGraph<Node, Edge> = Panel.Tree.generateTree(1)
            assertThat(actual.nodes()).hasSize(4)
            assertThat(actual.edges()).hasSize(3)

            val (leftTip: Node, rightTip: Node) = actual.tips()
            val root: Node = actual.root()
            val fork: Node = actual.adjacentNodes(root).single()

            assertThat(leftTip.x).isWithin(1e-3f).of(0.345f)
            assertThat(rightTip.x).isWithin(1e-3f).of(2.655f)
            assertThat(leftTip.y).isWithin(1e-3f).of(2.475f)
            assertThat(rightTip.y).isWithin(1e-3f).of(2.475f)
            assertThat(root.x).isWithin(1e-3f).of(1.5f)
            assertThat(root.y).isWithin(1e-3f).of(0.525f)
            assertThat(fork.x).isWithin(1e-3f).of(1.5f)
            assertThat(fork.y).isWithin(1e-3f).of(0.525f + 1.95f * 0.75f / 1.75f)
            assertThat(actual.adjacentNodes(fork)).containsExactly(root, leftTip, rightTip)
        }

        @Test
        fun `Test generate tree 2 tall`() {
            val actual: ValueGraph<Node, Edge> = Panel.Tree.generateTree(2)

            // 4 tips + 2 branches + 1 fork + 1 root, each branch joined up to its pair of children.
            assertThat(actual.nodes()).hasSize(8)
            assertThat(actual.edges()).hasSize(7)

            // Tips spread across the crown, level; the root is the single bottom node with one
            // branch, the trunk.
            val tips: List<Node> = actual.tips()
            assertThat(tips).hasSize(4)
            tips.forEach { tip -> assertThat(tip.y).isWithin(1e-5f).of(tips.first().y) }
            assertThat(tips.last().x - tips.first().x).isWithin(1e-3f).of(3 * Panel.Tree.CROWN_WIDTH)

            val root: Node = actual.root()
            assertThat(actual.degree(root)).isEqualTo(1)
            assertThat(root.x).isWithin(1e-5f).of(1.5f)

            // Every parent is centred under its two children.
            actual.nodes().filter { node -> node !in tips && node != root }.forEach { parent ->
                val children: List<Node> = actual.adjacentNodes(parent).filter { it.y > parent.y }
                assertThat(children).hasSize(2)
                assertThat(parent.x).isWithin(1e-5f).of(children.sumOf { it.x.toDouble() }.toFloat() / 2)
            }
        }

        @Test
        fun `Test generate tree 4 tall, the Orchard's 16 tips`() {
            val actual: ValueGraph<Node, Edge> = Panel.Tree.generateTree(4)
            assertThat(Panel.Tree.sizeFor(4)).isEqualTo(7)

            val tips: List<Node> = actual.tips()
            assertThat(tips).hasSize(16)
            // Tips fill 77% of a 7-unit panel: 1.44 line widths apart, the game's density.
            val gap: Float = 7 * Panel.Tree.CROWN_WIDTH / 15
            tips.zipWithNext().forEach { (a, b) -> assertThat(b.x - a.x).isWithin(1e-4f).of(gap) }
            assertThat(gap / 0.25f).isWithin(0.05f).of(1.44f)

            // Level heights follow the weights measured off the game (trunk 0.75, then 1, 1,
            // 0.6, 0.45), scaled to 65% of the panel. Pinned against the shots of 2026-09-05:
            // halving per level made a giant V, and filling the panel edge to edge looked nothing
            // like the Orchard.
            val rowYs: List<Float> = actual.nodes().map { it.y }.distinct().sorted()
            assertThat(rowYs).hasSize(6)
            val heights: List<Float> = rowYs.zipWithNext { lower, upper -> upper - lower }
            assertThat(heights.sum()).isWithin(1e-4f).of(7 * Panel.Tree.TREE_HEIGHT)
            heights.zip(Panel.Tree.LEVEL_WEIGHTS).zipWithNext { (h0, w0), (h1, w1) ->
                assertThat(h1 / h0).isWithin(1e-3f).of(w1 / w0)
            }
            // Centred: as much air below the root as above the tips.
            assertThat(rowYs.first()).isWithin(1e-4f).of(7 - rowYs.last())

            val root: Node = actual.root()
            assertThat(actual.adjacentNodes(root)).hasSize(1)
        }

        @Test
        fun `ofSize is in levels, sizes the panel to the crown, and ships the Orchard's marks`() {
            assertThat(Panel.Tree.ofSize(1).width).isEqualTo(3)
            assertThat(Panel.Tree.ofSize(2).width).isEqualTo(3)
            assertThat(Panel.Tree.ofSize(3).width).isEqualTo(4)
            val tree: Panel.Tree = Panel.Tree.ofSize(4)
            assertThat(tree.levels).isEqualTo(4)
            assertThat(tree.width).isEqualTo(7)
            assertThat(tree.height).isEqualTo(7)

            // A start on the root and an end straight up off every tip: 32 branch nodes + 16 nubs.
            assertThat(tree.graph.nodes()).hasSize(48)
            assertThat(tree.graph.root().modifier).isEqualTo(Modifier.START)
            val nubs: List<Node> = tree.graph.nubs()
            assertThat(nubs).hasSize(16)
            tree.graph.tips().forEach { tip ->
                val nub: Node = tree.graph.adjacentNodes(tip).single { it.modifier == Modifier.END }
                assertThat(nub.x).isEqualTo(tip.x)
                assertThat(nub.y).isWithin(1e-5f).of(tip.y + END_POINT_LENGTH)
            }
            // The nubs are what the frame reads as exits: all up.
            assertThat(tree.endSides()).containsExactly(Side.TOP)
        }

        @Test
        fun `growing carries marks by branch position and gives the new tips fresh ends`() {
            // A fresh Tree_1 (start on the root, ends on both tips) composed with a break on the
            // left branch and an apple on the left tip.
            val fresh: ValueGraph<Node, Edge> = Panel.Tree.ofSize(1).graph
            val root: Node = fresh.root()
            val fork: Node = fresh.adjacentNodes(root).single()
            val (leftTip: Node, rightTip: Node) = fresh.tips()
            val appleTip: Node = leftTip.copy(symbol = Atom.HEXAGON)
            val composed: MutableValueGraph<Node, Edge> = fresh
                .withNode(leftTip, appleTip)
                .apply { putEdgeValue(fork, appleTip, Edge.BREAK) }
            val source: Panel.Tree = Panel.Tree.ofSize(1).copy(graph = composed)

            val grown: Panel.Tree = source.expandTo(2)

            assertThat(grown.levels).isEqualTo(2)
            // Same shape as a fresh Tree_2: 8 branch nodes and a nub on each of the 4 tips, none
            // on the old tips.
            assertThat(grown.graph.branchNodes()).hasSize(8)
            assertThat(grown.graph.nubs()).hasSize(4)
            grown.graph.tips().forEach { tip ->
                assertThat(grown.graph.adjacentNodes(tip).count { it.modifier == Modifier.END }).isEqualTo(1)
            }

            // The root is still the start, and the trunk is still plain.
            val newRoot: Node = grown.graph.root()
            assertThat(newRoot.modifier).isEqualTo(Modifier.START)
            val newFork: Node = grown.graph.adjacentNodes(newRoot).single()
            assertThat(grown.graph.edgeValue(newRoot, newFork).get()).isEqualTo(Edge.NORMAL)

            // The old tips are the level-1 forks now, left to right; the left one keeps its apple
            // and lost its nub.
            val forks: List<Node> = grown.graph.adjacentNodes(newFork).filter { it.y > newFork.y }.sortedBy { it.x }
            assertThat(forks).hasSize(2)
            assertThat(forks[0].symbol).isEqualTo(Atom.HEXAGON)
            assertThat(forks[1].symbol).isEqualTo(Atom.NONE)
            forks.forEach { branch -> assertThat(grown.graph.adjacentNodes(branch).none { it.modifier == Modifier.END }).isTrue() }

            // The break stayed on the fork-to-left-branch edge and nowhere else.
            assertThat(grown.graph.edgeValue(newFork, forks[0]).get()).isEqualTo(Edge.BREAK)
            assertThat(grown.graph.edgeValue(newFork, forks[1]).get()).isEqualTo(Edge.NORMAL)
            forks.forEach { branch ->
                grown.graph.adjacentNodes(branch).filter { it.y > branch.y && it.modifier != Modifier.END }.forEach { tip ->
                    assertThat(grown.graph.edgeValue(branch, tip).get()).isEqualTo(Edge.NORMAL)
                }
            }
            assertThat(rightTip.modifier).isEqualTo(Modifier.NONE)
        }

        @Test
        fun `growing keeps a nub on the root`() {
            // An upside-down tree: start on a tip, end on the root.
            val blank: ValueGraph<Node, Edge> = Panel.Tree.generateTree(1)
            val root: Node = blank.root()
            val rootNub = Node(root.x, root.y - END_POINT_LENGTH, Modifier.END)
            val composed: MutableValueGraph<Node, Edge> = blank
                .withNode(blank.tips().first(), blank.tips().first().copy(modifier = Modifier.START))
                .apply { putEdgeValue(root, rootNub, Edge.NORMAL) }
            val grown: Panel.Tree = Panel.Tree.ofSize(1).copy(graph = composed).expandTo(2)

            val newRoot: Node = grown.graph.root()
            val rootNubs: List<Node> = grown.graph.adjacentNodes(newRoot).filter { it.modifier == Modifier.END }
            assertThat(rootNubs).hasSize(1)
            assertThat(rootNubs.single().x).isWithin(1e-5f).of(newRoot.x)
            assertThat(rootNubs.single().y).isWithin(1e-5f).of(newRoot.y - END_POINT_LENGTH)
            assertThat(grown.endSides()).containsExactly(Side.TOP, Side.BOTTOM)
        }

        @Test
        fun `growing a tree laid out before the trunk existed reads its root as the fork`() {
            // The pre-trunk Tree_1: a two-child root at (1, 0.5), tips at y = 1.5, start on the root.
            val root = Node(1f, 0.5f, Modifier.START)
            val leftTip = Node(0.5f, 1.5f, symbol = Atom.HEXAGON)
            val rightTip = Node(1.5f, 1.5f)
            val legacy: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected()
                .build<Node, Edge>().apply {
                    putEdgeValue(root, leftTip, Edge.BREAK)
                    putEdgeValue(root, rightTip, Edge.NORMAL)
                }
            val source = Panel.Tree(emptyGraph(), legacy, net.minecraft.world.item.DyeColor.WHITE, 2, 2, levels = 1)

            val grown: Panel.Tree = source.expandTo(2)

            val newRoot: Node = grown.graph.root()
            val newFork: Node = grown.graph.adjacentNodes(newRoot).single { it.modifier != Modifier.END }
            // The old root was the fork, so its start lands on the fork and the new foot is blank.
            assertThat(newRoot.modifier).isEqualTo(Modifier.NONE)
            assertThat(newFork.modifier).isEqualTo(Modifier.START)
            val forks: List<Node> = grown.graph.adjacentNodes(newFork).filter { it.y > newFork.y }.sortedBy { it.x }
            assertThat(forks[0].symbol).isEqualTo(Atom.HEXAGON)
            assertThat(grown.graph.edgeValue(newFork, forks[0]).get()).isEqualTo(Edge.BREAK)
        }

        @Test
        fun `growing a pruned tree keeps the pruning and the surviving limb's side`() {
            // Tree_2 with its LEFT limb pruned and an apple on the right limb's right tip.
            val fresh: Panel.Tree = Panel.Tree.ofSize(2)
            val fork: Node = fresh.graph.adjacentNodes(fresh.graph.root()).single { it.modifier != Modifier.END }
            val (left: Node, right: Node) = fresh.graph.adjacentNodes(fork).filter { it.y > fork.y }.sortedBy { it.x }
            val rightTips: List<Node> = fresh.graph.adjacentNodes(right).filter { it.y > right.y && it.modifier != Modifier.END }.sortedBy { it.x }
            val pruned: Panel.Tree = fresh.withNodeRemoved(left) as Panel.Tree
            val composed: Panel.Tree = pruned.copy(
                graph = Panel.Tree.run { com.google.common.graph.Graphs.copyOf(pruned.graph).withNodeReplaced(rightTips[1], rightTips[1].copy(symbol = Atom.HEXAGON)) }
            )

            val grown: Panel.Tree = composed.expandTo(3)

            val newFork: Node = grown.graph.adjacentNodes(grown.graph.root()).single { it.modifier != Modifier.END }
            val limbs: List<Node> = grown.graph.adjacentNodes(newFork).filter { it.y > newFork.y }.sortedBy { it.x }
            // Only the right limb, and it is on the right of the fork.
            assertThat(limbs).hasSize(1)
            assertThat(limbs.single().x).isGreaterThan(newFork.x)
            // The apple is on the right limb's right branch, now a fork with two ended tips.
            val branches: List<Node> = grown.graph.adjacentNodes(limbs.single()).filter { it.y > limbs.single().y }.sortedBy { it.x }
            assertThat(branches[1].symbol).isEqualTo(Atom.HEXAGON)
            assertThat(branches[0].symbol).isEqualTo(Atom.NONE)
            assertThat(grown.graph.nubs()).hasSize(4)
            // A Tree_3 has 32 nodes with nubs when full; half the crown is gone here.
            assertThat(grown.graph.branchNodes()).hasSize(2 + 1 + 2 + 4)
        }

        @Test
        fun `growing keeps a bare stub bare and an ended stub ended`() {
            // Tree_2 with both limbs pruned back to the fork: the fork is the only tip.
            val fresh: Panel.Tree = Panel.Tree.ofSize(2)
            val fork: Node = fresh.graph.adjacentNodes(fresh.graph.root()).single { it.modifier != Modifier.END }
            val limbs: List<Node> = fresh.graph.adjacentNodes(fork).filter { it.y > fork.y }
            val stub: Panel.Tree = limbs.fold(fresh as Panel) { panel, limb -> panel.withNodeRemoved(limb) } as Panel.Tree

            val grownBare: Panel.Tree = stub.expandTo(3)
            val newFork: Node = grownBare.graph.adjacentNodes(grownBare.graph.root()).single { it.modifier != Modifier.END }
            assertThat(grownBare.graph.adjacentNodes(newFork).filter { it.y > newFork.y }).isEmpty()
            assertThat(grownBare.graph.nubs()).isEmpty()

            val ended: Panel.Tree = requireNotNull(stub.withEndPointToggled(fork)) as Panel.Tree
            val grownEnded: Panel.Tree = ended.expandTo(3)
            assertThat(grownEnded.graph.nubs()).hasSize(1)
            val endedFork: Node = grownEnded.graph.adjacentNodes(grownEnded.graph.root()).single { it.modifier != Modifier.END }
            assertThat(grownEnded.graph.adjacentNodes(endedFork).filter { it.modifier == Modifier.END }).hasSize(1)
        }

        @Test
        fun `growing to the same or a smaller size is a no-op`() {
            val tree: Panel.Tree = Panel.Tree.ofSize(2)
            assertThat(tree.expandTo(2)).isSameInstanceAs(tree)
            assertThat(tree.expandTo(1)).isSameInstanceAs(tree)
        }
    }

    @Nested
    @DisplayName("Test nearest logic")
    inner class TestNearestLogic {

        @Test
        fun `Test nearest node`() {
            val actual: Node? = testGraph.nearestNode(0f, 0.6f)
            val expect: Node = topRight
            assertThat(actual).isEqualTo(expect)
        }

        @Test
        fun `Test nearest node from bottom right to bottom right`() {
            val actual: Node? = testGraph.nearestNode(0.1f, 0.1f, bottomRight)
            val expect: Node = bottomRight
            assertThat(actual).isEqualTo(expect)
        }

        @Test
        fun `Test nearest node from bottom right to top left`() {
            val actual: Node? = testGraph.nearestNode(0.5f, 0.6f, bottomRight)
            val expect: Node = topRight
            assertThat(actual).isEqualTo(expect)
        }

        @Test
        fun `Test closest from top right`() {
            val actual: Pair<Float, Float> = getClosest(bottomRight, topLeft, topRight)
            val expect: Pair<Float, Float> = 0.5f to 0.5f
            assertThat(actual).isEqualTo(expect)
        }

        @Test
        fun `Test closest from bottom left`() {
            val actual: Pair<Float, Float> = getClosest(bottomRight, topLeft, bottomLeft)
            val expect: Pair<Float, Float> = 0.5f to 0.5f
            assertThat(actual).isEqualTo(expect)
        }

        @Test
        fun `Test closest from  top left`() {
            val actual: Pair<Float, Float> = getClosest(topRight, bottomRight, topLeft)
            val expect: Pair<Float, Float> = 0f to 1f
            assertThat(actual).isEqualTo(expect)
        }

        @Test
        fun `Test nearest Edge from bottom right to middle`() {
            val actual: EdgeResult? = testGraph.nearestEdge(.5f, .5f, bottomRight)
            val expect = EdgeResult(.0f, .5f, EndpointPair.unordered(topRight, bottomRight))
            assertThat(actual).isEqualTo(expect)
        }

        @Test
        fun `Nearest edge is incident to the current node`() {
            val actual: EdgeResult? = testGraph.nearestEdge(.9f, 1f, bottomRight)
            val expect = EdgeResult(0f, 1f, EndpointPair.unordered(bottomRight, topRight))
            assertThat(actual).isEqualTo(expect)
        }

        @Test
        fun `Closest point is clamped to the edge segment`() {
            val actual: Pair<Float, Float> = getClosest(bottomRight, topRight, Node(0f, 2f))
            assertThat(actual).isEqualTo(0f to 1f)
        }
    }
}
