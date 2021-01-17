package com.xfastgames.witness.utils

import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph

@Suppress("UnstableApiUsage")
val <N, E> ValueGraph<N, E>.adjacencyMatrix: List<List<E?>>
    get() = this.nodes().map { thisNode ->
        this.nodes().map { thatNode ->
            this.edgeValue(thisNode, thatNode).value
        }
    }

@Suppress("UnstableApiUsage")
inline fun <N, reified E> ValueGraph<N, E>.edgeValues(): List<E> =
    this.edges().map { nodePair -> this.edgeValue(nodePair).orElse(null) }.filterIsInstance<E>()

/***
 * Adds a [nodeList] to a existing graph with the given [adjacencyMatrix]
 */
@Suppress("UnstableApiUsage")
fun <N, E> MutableValueGraph<N, E>.add(nodeList: List<N>, adjacencyMatrix: List<List<E?>>): MutableValueGraph<N, E> =
    this.apply {
        adjacencyMatrix.forEachIndexed { thisIndex, edges ->
            val thisNode: N = nodeList[thisIndex]
            edges.forEachIndexed { thatIndex, edge ->
                val thatNode: N = nodeList[thatIndex]
                edge?.let { this.putEdgeValue(thisNode, thatNode, edge) }
            }
        }
    }
