package com.xfastgames.witness.items.data

import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.xfastgames.witness.utils.add
import com.xfastgames.witness.utils.adjacencyMatrix
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag

private const val KEY_GRAPH = "graph"
private const val KEY_EDGES = "edges"
private const val KEY_NODES = "nodes"
private const val KEY_NODE_X = "x"
private const val KEY_NODE_Y = "y"
private const val KEY_NODE_MODIFIER = "modifier"

enum class Modifier { EMPTY, NORMAL, BREAK, DOT, START, END }

data class Node(val x: Float, val y: Float, val modifier: Modifier = Modifier.EMPTY)
typealias Edge = Modifier

@Suppress("UnstableApiUsage")
fun generateGrid(size: Int): ValueGraph<Node, Edge> {
    val graph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected().build()

    val previousRow: MutableList<Node> = mutableListOf()
    repeat(size) { x ->
        var previousNode: Node? = null
        val currentRow: MutableList<Node> = mutableListOf()

        repeat(size) { y ->
            val (dx, dy) = x.toFloat() to y.toFloat()
            val currentNode = Node(dx, dy)
            graph.addNode(currentNode)
            currentRow.add(currentNode)

            // Link horizontal neighbour
            previousNode?.let { node -> graph.putEdgeValue(node, currentNode, Modifier.NORMAL) }

            // Link vertical neighbour
            previousRow.takeIf { it.isNotEmpty() }
                ?.let { row -> graph.putEdgeValue(row[y], currentNode, Modifier.NORMAL) }

            previousNode = currentNode
        }

        previousNode = null
        previousRow.clear()
        previousRow.addAll(currentRow)
        currentRow.clear()
    }

    return graph
}

fun CompoundTag.getNode() = Node(
    x = getFloat(KEY_NODE_X),
    y = getFloat(KEY_NODE_Y),
    modifier = getInt(KEY_NODE_MODIFIER)
        .let { Modifier.values()[it] }
)

fun CompoundTag.putNode(node: Node) {
    putFloat(KEY_NODE_X, node.x)
    putFloat(KEY_NODE_Y, node.y)
    putInt(KEY_NODE_MODIFIER, node.modifier.ordinal)
}

@Suppress("UnstableApiUsage")
fun CompoundTag.getGraph(): ValueGraph<Node, Edge> =
    getCompound(KEY_GRAPH).let { tag ->
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
                        .map { modifier -> if (modifier == Modifier.EMPTY) null else modifier }
                        .chunked(nodes.size)

                add(nodes, adjacencyMatrix)
            }
    }

@Suppress("UnstableApiUsage")
fun CompoundTag.putGraph(graph: ValueGraph<Node, Edge>) {
    put(KEY_GRAPH, CompoundTag().apply {
        val nodes: Set<Node> = graph.nodes()
        put(KEY_NODES, ListTag().apply {
            nodes.forEach { node ->
                add(CompoundTag().apply { putNode(node) })
            }
        })
        putIntArray(KEY_EDGES, IntArray(nodes.size * nodes.size).apply {
            graph.adjacencyMatrix.flatten().forEachIndexed { index, edge ->
                val edgeToAdd: Int = edge?.ordinal ?: Edge.EMPTY.ordinal
                this[index] = edgeToAdd
            }
        })
    })
}

