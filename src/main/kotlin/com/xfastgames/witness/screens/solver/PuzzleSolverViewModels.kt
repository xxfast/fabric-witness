package com.xfastgames.witness.screens.solver

import com.google.common.graph.ValueGraph
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.data.Edge
import com.xfastgames.witness.items.data.Hexagon
import com.xfastgames.witness.items.data.Node
import com.xfastgames.witness.items.data.Panel

@Suppress("UnstableApiUsage")
sealed class PuzzleSolverEvent {
    data class SelectNode(val node: Node, val graph: ValueGraph<Node, Edge>) : PuzzleSolverEvent()
    object DeselectNode : PuzzleSolverEvent()
}

sealed class PuzzleSolverData {
    object PreSolution : PuzzleSolverData()
    object InSolution : PuzzleSolverData()
    object SolutionSubmitted : PuzzleSolverData()

    /** The line was released somewhere other than an end point, so nothing was ever submitted. */
    object SolutionAborted : PuzzleSolverData()

    /**
     * The path reached an end but failed validation. [missedHexagons] is the subset of hexagon dots
     * the path never covered (empty when the reject is structural rather than rule 04).
     */
    data class SolutionRejected(val missedHexagons: List<Hexagon> = emptyList()) : PuzzleSolverData()

    object SolutionAccepted : PuzzleSolverData()
}

data class PuzzlePanelHitResult(
    val position: Pair<Float, Float>,
    val puzzlePanel: Panel,
    val blockEntity: PuzzleFrameBlockEntity,
)