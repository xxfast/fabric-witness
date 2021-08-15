package com.xfastgames.witness.items.data

import com.google.common.graph.EndpointPair
import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.google.common.truth.Truth.assertThat
import net.minecraft.nbt.NbtCompound
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
            putEdgeValue(bottomRight, topRight, Modifier.NORMAL)
            putEdgeValue(topRight, topLeft, Modifier.NORMAL)
            putEdgeValue(topLeft, bottomLeft, Modifier.NORMAL)
            putEdgeValue(bottomLeft, bottomRight, Modifier.NORMAL)
        }

    @Test
    fun `Test put and read grid`() {
        val tag: NbtCompound = NbtCompound().apply { putValueGraph(KEY_GRAPH, testGraph) }
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

                    putEdgeValue(bottomLeft, bottomRight, Modifier.NORMAL)
                    putEdgeValue(topLeft, bottomLeft, Modifier.NORMAL)
                    putEdgeValue(topRight, topLeft, Modifier.NORMAL)
                    putEdgeValue(topRight, bottomRight, Modifier.NORMAL)
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

                    putEdgeValue(bottomLeft, bottomMiddle, Modifier.NORMAL)
                    putEdgeValue(bottomMiddle, bottomRight, Modifier.NORMAL)
                    putEdgeValue(middleLeft, middleMiddle, Modifier.NORMAL)
                    putEdgeValue(middleMiddle, middleRight, Modifier.NORMAL)
                    putEdgeValue(topLeft, topMiddle, Modifier.NORMAL)
                    putEdgeValue(topMiddle, topRight, Modifier.NORMAL)
                    putEdgeValue(topLeft, middleLeft, Modifier.NORMAL)
                    putEdgeValue(middleLeft, bottomLeft, Modifier.NORMAL)
                    putEdgeValue(topMiddle, middleMiddle, Modifier.NORMAL)
                    putEdgeValue(middleMiddle, bottomMiddle, Modifier.NORMAL)
                    putEdgeValue(topRight, middleRight, Modifier.NORMAL)
                    putEdgeValue(middleRight, bottomRight, Modifier.NORMAL)
                }

            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("Test tree generation")
    inner class TestTreeGeneration {

        @Test
        fun `Test generate tree 1 tall`() {
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
    }
}