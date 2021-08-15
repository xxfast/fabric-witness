@file:Suppress("UnstableApiUsage")

package com.xfastgames.witness.utils.guava

import com.google.common.graph.EndpointPair
import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder

fun <N : Any, E : Any> ValueGraph<N, E>.edgeValueOf(u: N, v: N): E? =
    edgeValueOrDefault(u, v, null)

fun <N : Any, E : Any> ValueGraph<N, E>.edgeValueOf(pair: EndpointPair<N>): E? =
    edgeValueOf(pair.nodeU(), pair.nodeV())

fun <N : Any, E : Any> ValueGraph<N, E>.incidentEdges(node: N): List<EndpointPair<N>> =
    this.edges().filter { endpointPair -> endpointPair.contains(node) }

val <N : Any, E : Any> ValueGraph<N, E>.adjacencyMatrix: List<List<E?>>
    get() = this.nodes().map { thisNode ->
        this.nodes().map { thatNode -> this.edgeValueOf(thisNode, thatNode) }
    }

inline fun <N : Any, reified E : Any> ValueGraph<N, E>.edgeValues(): List<E> =
    this.edges()
        .map { nodePair -> this.edgeValue(nodePair.nodeU(), nodePair.nodeV()) }
        .filterIsInstance<E>()

/***
 * Adds a [nodeList] to a existing graph with the given [adjacencyMatrix]
 */
fun <N, E> MutableValueGraph<N, E>.add(nodeList: List<N>, adjacencyMatrix: List<List<E?>>): MutableValueGraph<N, E> =
    this.apply {
        nodeList.forEach { this.addNode(it) }
        adjacencyMatrix.forEachIndexed { thisIndex, edges ->
            val thisNode: N = nodeList[thisIndex]
            edges.forEachIndexed { thatIndex, edge ->
                val thatNode: N = nodeList[thatIndex]
                edge?.let { this.putEdgeValue(thisNode, thatNode, edge) }
            }
        }
    }

fun <N, E> mutableValueGraph(): MutableValueGraph<N, E> = ValueGraphBuilder
    .undirected()
    .build()
