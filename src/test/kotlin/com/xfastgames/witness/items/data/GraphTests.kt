package com.xfastgames.witness.items.data

import com.google.common.graph.EndpointPair
import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.google.common.truth.Truth.assertThat
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

        @Test
        fun `Test generate tree 1 tall`() {
            val actual: ValueGraph<Node, Edge> = Panel.Tree.generateTree(1)
            val expected: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected()
                .build<Node, Edge>().apply {
                    val leftTip = Node(0.5f, 1.5f)
                    val rightTip = Node(1.5f, 1.5f)
                    val root = Node(1f, 0.5f)

                    putEdgeValue(root, leftTip, Edge.NORMAL)
                    putEdgeValue(root, rightTip, Edge.NORMAL)
                }

            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `Test generate tree 2 tall`() {
            val actual: ValueGraph<Node, Edge> = Panel.Tree.generateTree(2)

            // 4 tips + 2 branches + 1 root, each branch joined up to its pair of children.
            assertThat(actual.nodes()).hasSize(7)
            assertThat(actual.edges()).hasSize(6)

            // Tips sit a row apart on the top row, spread across the panel inside the half unit
            // margin; the single bottom node is the root.
            val tips: List<Node> = actual.nodes().filter { node -> actual.degree(node) == 1 }
            assertThat(tips).hasSize(4)
            tips.forEach { tip -> assertThat(tip.y).isEqualTo(2.5f) }
            assertThat(tips.minOf { it.x }).isEqualTo(0.5f)
            assertThat(tips.maxOf { it.x }).isEqualTo(2.5f)

            val root: Node = actual.nodes().single { node -> node.y == 0.5f }
            assertThat(actual.degree(root)).isEqualTo(2)
            assertThat(root.x).isWithin(1e-5f).of(1.5f)

            // Every parent is centred under its two children.
            actual.nodes().filter { node -> node !in tips }.forEach { parent ->
                val children: List<Node> = actual.adjacentNodes(parent).filter { it.y > parent.y }
                assertThat(children).hasSize(2)
                assertThat(parent.x).isWithin(1e-5f).of(children.sumOf { it.x.toDouble() }.toFloat() / 2)
            }
        }

        @Test
        fun `ofSize is in levels and sizes the panel one unit per node row`() {
            val tree: Panel.Tree = Panel.Tree.ofSize(2)
            assertThat(tree.width).isEqualTo(3)
            assertThat(tree.height).isEqualTo(3)
            assertThat(tree.graph.nodes()).hasSize(7)
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
