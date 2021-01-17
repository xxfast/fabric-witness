package com.xfastgames.witness.items.data

import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.xfastgames.witness.utils.add
import com.xfastgames.witness.utils.adjacencyMatrix
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.ListTag

private const val KEY_EDGES = "edges"
private const val KEY_NODES = "nodes"
private const val KEY_FILL = "fill"

@Suppress("UnstableApiUsage")
fun CompoundTag.getEdgeGraph(key: String): ValueGraph<Node, Edge> =
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
fun CompoundTag.putEdgeGraph(key: String, graph: ValueGraph<Node, Edge>) {
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
fun CompoundTag.getFloatGraph(key: String): ValueGraph<Node, Float> =
    getCompound(key).let { tag ->
        ValueGraphBuilder
            .undirected()
            .build<Node, Float>()
            .apply {
                if (tag.isEmpty) return@apply

                val nodes: List<Node> = tag.getList(KEY_NODES, 10)
                    .filterIsInstance<CompoundTag>()
                    .map { it.getNode() }

                if (nodes.isEmpty()) return@apply

                val adjacencyMatrix: List<List<Float?>> =
                    tag.getList(KEY_FILL, 10)
                        .filterIsInstance<FloatTag>()
                        .map { tag -> tag.float }
                        .map { value -> if (value == 0f) null else value }
                        .chunked(nodes.size)

                add(nodes, adjacencyMatrix)
            }
    }

@Suppress("UnstableApiUsage")
fun CompoundTag.putFloatGraph(key: String, graph: ValueGraph<Node, Float>) {
    put(key, CompoundTag().apply {
        val nodes: Set<Node> = graph.nodes()
        put(KEY_NODES, ListTag().apply {
            nodes.forEach { node ->
                add(CompoundTag().apply { putNode(node) })
            }
        })
        put(KEY_FILL, ListTag().apply {
            graph.adjacencyMatrix.flatten().forEachIndexed { index, value ->
                value?.let { this[index] = FloatTag.of(value) }
            }
        })
    })
}

