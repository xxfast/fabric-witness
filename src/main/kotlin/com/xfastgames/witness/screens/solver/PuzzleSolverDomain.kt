package com.xfastgames.witness.screens.solver

import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.xfastgames.witness.items.data.Node
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.utils.mutableValueGraph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Suppress("UnstableApiUsage")
@FlowPreview
@OptIn(ExperimentalCoroutinesApi::class)
class PuzzleSolverDomain {
    private val stateFlow: MutableStateFlow<PuzzleSolverData> =
        MutableStateFlow(PuzzleSolverData.PreSolution)

    val state: StateFlow<PuzzleSolverData> = stateFlow

    fun startTracingLine(panel: Panel, start: Node): ValueGraph<Node, Float>? {
        stateFlow.value = PuzzleSolverData.InSolution
        if (!panel.graph.nodes().contains(start)) return null
        val graph: MutableValueGraph<Node, Float> = mutableValueGraph()
        graph.addNode(start)
        return graph
    }

    fun introduceWaypoint(panel: Panel) {
    }

    fun stopTrace() {
        stateFlow.value = PuzzleSolverData.PreSolution
    }

    val isSolving: Boolean get() = stateFlow.value == PuzzleSolverData.InSolution
}