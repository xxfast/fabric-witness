@file:Suppress("UnstableApiUsage")

package com.xfastgames.witness.items.data

import com.google.common.graph.*
import com.xfastgames.witness.utils.guava.add
import com.xfastgames.witness.utils.guava.adjacencyMatrix
import net.minecraft.nbt.*
import kotlin.math.pow

private const val KEY_EDGES = "edges"
private const val KEY_NODES = "nodes"
private const val KEY_FILL = "fill"

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

fun ValueGraph<Node, Edge>.nearestNode(x: Float, y: Float, from: Node? = null): Node? {
    val imaginaryNode = Node(x, y)
    return nodes()
        .minByOrNull { node ->
            val distanceToNode: Float = distance(imaginaryNode, node)
            val distanceToFrom: Float = from?.let { distance(imaginaryNode, from) } ?: 0f
            distanceToNode + distanceToFrom
        }
}

data class EdgeResult(val x: Float, val y: Float, val edge: EndpointPair<Node>)

fun ValueGraph<Node, Edge>.nearestEdge(x: Float, y: Float, from: Node? = null): EdgeResult? {
    val imaginaryNode = Node(x, y)
    val nearestEdge: EndpointPair<Node> = edges().minByOrNull { endpointPair ->
        val u: Node = endpointPair.nodeU()
        val v: Node = endpointPair.nodeV()
        val sumDistance: Float = distance(u, imaginaryNode) + distance(imaginaryNode, v)
        val distance: Float = distance(u, v)
        sumDistance - distance
    } ?: return null

    val u: Node = nearestEdge.nodeU()
    val v: Node = nearestEdge.nodeV()
    val (dx, dy) = getClosest(u, v, imaginaryNode)
    return EdgeResult(dx, dy, nearestEdge)
}

fun getClosest(a: Node, b: Node, p: Node): Pair<Float, Float> {
    val aToP = p.x - a.x to p.y - a.y
    val aToB = b.x - a.x to b.y - a.y
    val aToBSquared = aToB.first.pow(2) + aToB.second.pow(2)
    val aToPDotAtoB = aToP.first * aToB.first + aToP.second * aToB.second
    val t: Float = aToPDotAtoB / aToBSquared
    return (a.x + aToB.first * t) to (a.y + aToB.second * t)
}

