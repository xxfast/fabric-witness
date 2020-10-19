package com.xfastgames.witness.utils

import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.items.data.*
import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.Test

@Suppress("UnstableApiUsage")
class GraphTests {

    private val testGraph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected()
        .build<Node, Edge>().apply {
            putEdgeValue(Node(0f, 0f, Modifier.START), Node(0f, 1f), Modifier.NORMAL)
            putEdgeValue(Node(0f, 1f), Node(1f, 1f, Modifier.END), Modifier.NORMAL)
        }

    @Test
    fun `test put and read graph`() {
        val tag: CompoundTag = CompoundTag().apply { putGraph(testGraph) }
        println(tag)
        val actual: ValueGraph<Node, Edge> = tag.getGraph()
        assertThat(actual).isEqualTo(testGraph)
    }
}