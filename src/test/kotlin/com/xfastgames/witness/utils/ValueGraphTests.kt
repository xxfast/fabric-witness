package com.xfastgames.witness.utils

import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.utils.guava.add
import com.xfastgames.witness.utils.guava.adjacencyMatrix
import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

const val KEY_GRAPH = "testGraph"

enum class Person { Bob, Alice, Rob, Maria, Mark }

@Suppress("UnstableApiUsage")
class ValueGraphTests {

    private val testGraph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected()
        .build<Node, Edge>().apply {
            putEdgeValue(Node(0f, 0f, Modifier.START), Node(0f, 1f), Modifier.NORMAL)
            putEdgeValue(Node(0f, 1f), Node(1f, 1f, Modifier.END), Modifier.NORMAL)
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
    inner class ValueGraphTests {
        private val testNodes: List<Person> = Person.values().toList()

        private val testAdjacencyMatrix: List<List<Double?>> = listOf(
            //    Bob,      Alice,  Rob,    Maria,  Mark
            listOf(null, 1.0, 0.75, null, null),  // Bob
            listOf(1.0, null, null, 0.75, 0.5),   // Alice
            listOf(0.75, null, null, 1.0, 0.5),   // Rob
            listOf(null, 0.75, 1.0, null, null),  // Maria
            listOf(null, 0.5, 0.5, null, null)   // Mark
        )

        /**
         * Based on https://www.baeldung.com/wp-content/uploads/2018/11/graph3.jpg
         */
        private val testPersonGraph: MutableValueGraph<Person, Double> = ValueGraphBuilder.undirected()
            .build<Person, Double>().apply {
                putEdgeValue(Person.Bob, Person.Alice, 1.0)
                putEdgeValue(Person.Bob, Person.Rob, 0.75)
                putEdgeValue(Person.Alice, Person.Maria, 0.75)
                putEdgeValue(Person.Maria, Person.Rob, 1.0)
                putEdgeValue(Person.Alice, Person.Mark, .5)
                putEdgeValue(Person.Mark, Person.Rob, .5)
            }

        @Test
        fun `Test if the network adjacency matrix is setup correct`() {
            val actual: List<List<Double?>> = testPersonGraph.adjacencyMatrix
            assertThat(actual).isEqualTo(testAdjacencyMatrix)
        }

        @Test
        fun `Test if the network can be build from a set of nodes and adjacency matrix`() {
            val actual: MutableValueGraph<Person, Double> = ValueGraphBuilder.undirected().build()
            actual.add(testNodes, testAdjacencyMatrix)
            assertThat(actual).isEqualTo(testPersonGraph)
        }
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
}