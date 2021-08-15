package com.xfastgames.witness.utils.guava

import com.google.common.graph.Graph

@Suppress("UnstableApiUsage")
class Traverser<N : Any>(private val graph: Graph<N>) {

    fun depthFirst(start: N, traversedNodes: MutableList<N> = mutableListOf(start)): List<N> {
        val adjacentNodes: MutableSet<N> = graph.adjacentNodes(start)
        val nextNode: N? = adjacentNodes.firstOrNull { node -> node !in traversedNodes }
        if (nextNode != null) traversedNodes.add(nextNode)
        else return traversedNodes
        return depthFirst(nextNode, traversedNodes)
    }

    companion object {
        fun <N : Any> forGraph(graph: Graph<N>): Traverser<N> = Traverser(graph)
    }
}