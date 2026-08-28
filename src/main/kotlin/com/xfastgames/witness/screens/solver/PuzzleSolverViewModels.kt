package com.xfastgames.witness.screens.solver

import com.google.common.graph.ValueGraph
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.data.CellSymbol
import com.xfastgames.witness.items.data.Edge
import com.xfastgames.witness.items.data.Hexagon
import com.xfastgames.witness.items.data.Node
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.renderer.PanelErrorFlash

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
     * the path never covered (rule 04) and [clashingSquares] the squares left sharing a region with
     * another colour (rule 06); both empty when the reject is structural.
     */
    data class SolutionRejected(
        val missedHexagons: List<Hexagon> = emptyList(),
        val clashingSquares: List<CellSymbol> = emptyList(),
    ) : PuzzleSolverData() {
        /** Where every failed symbol sits and what shape it is, for the error flash. */
        val failedMarks: List<PanelErrorFlash.Mark>
            get() = missedHexagons.map { hexagon ->
                when (hexagon) {
                    is Hexagon.OnNode ->
                        PanelErrorFlash.Mark(hexagon.node.x, hexagon.node.y, PanelErrorFlash.Shape.HEXAGON)
                    is Hexagon.OnEdge -> PanelErrorFlash.Mark(
                        (hexagon.u.x + hexagon.v.x) / 2f,
                        (hexagon.u.y + hexagon.v.y) / 2f,
                        PanelErrorFlash.Shape.HEXAGON
                    )
                }
            } + clashingSquares.map { square ->
                PanelErrorFlash.Mark(square.x, square.y, PanelErrorFlash.Shape.SQUARE)
            }
    }

    object SolutionAccepted : PuzzleSolverData()
}

data class PuzzlePanelHitResult(
    val position: Pair<Float, Float>,
    val puzzlePanel: Panel,
    val blockEntity: PuzzleFrameBlockEntity,
)