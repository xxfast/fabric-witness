package com.xfastgames.witness.utils.guava

import com.google.common.graph.EndpointPair
import com.google.common.graph.MutableValueGraph

@Suppress("UnstableApiUsage")
fun <N : Any, E : Any> MutableValueGraph<N, E>.putEdgeValue(pair: EndpointPair<N>, edge: E) =
    this.putEdgeValue(pair.nodeU(), pair.nodeV(), edge)