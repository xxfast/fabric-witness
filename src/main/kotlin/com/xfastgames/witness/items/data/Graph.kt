package com.xfastgames.witness.items.data

import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.xfastgames.witness.utils.guava.add
import com.xfastgames.witness.utils.guava.adjacencyMatrix
import net.minecraft.nbt.*

private const val KEY_EDGES = "edges"
private const val KEY_NODES = "nodes"
private const val KEY_FILL = "fill"

@Suppress("UnstableApiUsage")
fun CompoundTag.getValueGraph(key: String): ValueGraph<Node, Edge> =
    getCompound(key).let { tag ->
        ValueGraphBuilder
            .undirected()
            .build<Node, Edge>()
            .apply {
                if (tag.isEmpty) return@apply

                val nodes: List<Node> = tag.getList(KEY_NODES, 10)
                    .filterIsInstance<CompoundTag>()
                    .map { it.getNode() }

                if (nodes.isEmpty()) return@apply

                val adjacencyMatrix =
                    tag.getIntArray(KEY_EDGES)
                        .map { index -> Modifier.values()[index] }
                        .map { modifier -> if (modifier == Modifier.NONE) null else modifier }
                        .chunked(nodes.size)

                add(nodes, adjacencyMatrix)
            }
    }

@Suppress("UnstableApiUsage")
fun CompoundTag.putValueGraph(key: String, graph: ValueGraph<Node, Edge>) {
    put(key, CompoundTag().apply {
        val nodes: Set<Node> = graph.nodes()
        put(KEY_NODES, ListTag().apply {
            nodes.forEach { node ->
                add(CompoundTag().apply { putNode(node) })
            }
        })
        putIntArray(KEY_EDGES, IntArray(nodes.size * nodes.size).apply {
            graph.adjacencyMatrix.flatten().forEachIndexed { index, edge ->
                val edgeToAdd: Int = edge?.ordinal ?: Edge.NONE.ordinal
                this[index] = edgeToAdd
            }
        })
    })
}

@Suppress("UnstableApiUsage")
fun CompoundTag.getGraph(key: String): Graph<Node> =
    getCompound(key).let { tag ->
        GraphBuilder
            .undirected()
            .build<Node>()
            .apply {
                if (tag.isEmpty) return@apply

                val nodes: List<Node> = tag.getList(KEY_NODES, 10)
                    .filterIsInstance<CompoundTag>()
                    .map { it.getNode() }

                if (nodes.isEmpty()) return@apply

                val adjacencyMatrix: List<List<Boolean>> =
                    tag.getIntArray(KEY_FILL)
                        .map { value -> value != 0 }
                        .chunked(nodes.size)

                add(nodes, adjacencyMatrix)
            }
    }

@Suppress("UnstableApiUsage")
fun CompoundTag.putGraph(key: String, graph: Graph<Node>) {
    put(key, CompoundTag().apply {
        val nodes: Set<Node> = graph.nodes()
        put(KEY_NODES, ListTag().apply {
            nodes.forEach { node ->
                add(CompoundTag().apply { putNode(node) })
            }
        })
        put(KEY_FILL, IntArrayTag(graph.adjacencyMatrix.flatten().map { value -> if (value) 1 else 0 }))
    })
}

