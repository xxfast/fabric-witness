package com.xfastgames.witness.utils

import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.items.data.*
import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("UnstableApiUsage")
class GridTests {

    private val testGraph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected()
        .build<Node, Edge>().apply {
            putEdgeValue(Node(0f, 0f, Modifier.START), Node(0f, 1f), Modifier.NORMAL)
            putEdgeValue(Node(0f, 1f), Node(1f, 1f, Modifier.END), Modifier.NORMAL)
        }

    @Test
    fun `Test put and read grid`() {
        val tag: CompoundTag = CompoundTag().apply { putGraph(testGraph) }
        println(tag)
        val actual: ValueGraph<Node, Edge> = tag.getGraph()
        assertThat(actual).isEqualTo(testGraph)
    }

    @Nested
    @DisplayName("Test grid generation")
    inner class TestGridGeneration {

        @Test
        fun `Test generate grid 2x2`() {
            val actual: ValueGraph<Node, Edge> = generateGrid(2)
            val expected: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected()
                .build<Node, Edge>().apply {
                    val bottomLeft = Node(0f, 0f)
                    val bottomRight = Node(0f, 1f)
                    val topLeft = Node(1f, 0f)
                    val topRight = Node(1f, 1f)

                    putEdgeValue(bottomLeft, bottomRight, Modifier.NORMAL)
                    putEdgeValue(topLeft, bottomLeft, Modifier.NORMAL)
                    putEdgeValue(topRight, topLeft, Modifier.NORMAL)
                    putEdgeValue(topRight, bottomRight, Modifier.NORMAL)
                }

            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `Test generate grid 3x3`() {
            val actual: ValueGraph<Node, Edge> = generateGrid(3)
            val expected: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected()
                .build<Node, Edge>().apply {
                    val bottomLeft = Node(0f, 0f)
                    val bottomMiddle = Node(0f, 1f)
                    val bottomRight = Node(0f, 2f)

                    val middleLeft = Node(1f, 0f)
                    val middleMiddle = Node(1f, 1f)
                    val middleRight = Node(1f, 2f)

                    val topLeft = Node(2f, 0f)
                    val topMiddle = Node(2f, 1f)
                    val topRight = Node(2f, 2f)

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


}