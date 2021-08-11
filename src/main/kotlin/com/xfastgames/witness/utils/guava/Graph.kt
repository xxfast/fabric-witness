@file:Suppress("UnstableApiUsage")

package com.xfastgames.witness.utils.guava

import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import com.google.common.graph.MutableGraph

val <N> Graph<N>.adjacencyMatrix: List<List<Boolean>>
    get() = this.nodes().map { thisNode ->
        this.nodes().map { thatNode ->
            this.hasEdgeConnecting(thisNode, thatNode)
        }
    }

/***
 * Adds a [nodeList] to a existing graph with the given [adjacencyMatrix]
 */
fun <N : Any> MutableGraph<N>.add(nodeList: List<N>, adjacencyMatrix: List<List<Boolean>>): MutableGraph<N> =
    this.apply {
        nodeList.forEach { node -> this.addNode(node) }
        adjacencyMatrix.forEachIndexed { thisIndex, edges ->
            val thisNode: N = nodeList[thisIndex]
            edges.forEachIndexed { thatIndex, edge ->
                val thatNode: N = nodeList[thatIndex]
                if (edge) this.putEdge(thisNode, thatNode)
            }
        }
    }

fun <N : Any> MutableGraph<N>.clear() {
    val nodes: List<N> = this.nodes().toList()
    nodes.forEach { removeNode(it) }
}

fun <N : Any> mutableGraph(): MutableGraph<N> = GraphBuilder
    .undirected()
    .build()

fun <N : Any> mutableGraph(from: Graph<N>): MutableGraph<N> {
    val graph: MutableGraph<N> = GraphBuilder.from(from).build()
    graph.add(from.nodes().toList(), from.adjacencyMatrix)
    return graph
}

fun <N : Any> emptyGraph(): Graph<N> = mutableGraph()