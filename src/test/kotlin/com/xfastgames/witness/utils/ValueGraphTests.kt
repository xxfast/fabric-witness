package com.xfastgames.witness.utils

import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.google.common.truth.Truth
import com.xfastgames.witness.items.data.Person
import com.xfastgames.witness.utils.guava.add
import com.xfastgames.witness.utils.guava.adjacencyMatrix
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("UnstableApiUsage")
class ValueGraphTests {

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
            Truth.assertThat(actual).isEqualTo(testAdjacencyMatrix)
        }

        @Test
        fun `Test if the network can be build from a set of nodes and adjacency matrix`() {
            val actual: MutableValueGraph<Person, Double> = ValueGraphBuilder.undirected().build()
            actual.add(testNodes, testAdjacencyMatrix)
            Truth.assertThat(actual).isEqualTo(testPersonGraph)
        }
    }
}