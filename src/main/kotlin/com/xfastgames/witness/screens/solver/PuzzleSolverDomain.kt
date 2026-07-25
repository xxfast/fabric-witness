package com.xfastgames.witness.screens.solver

import com.google.common.graph.Graph
import com.google.common.graph.MutableGraph
import com.xfastgames.witness.items.data.Modifier
import com.xfastgames.witness.items.data.Node
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.utils.guava.mutableGraph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

@Suppress("UnstableApiUsage")
@FlowPreview
@OptIn(ExperimentalCoroutinesApi::class)
class PuzzleSolverDomain {
    private data class ActiveSegment(
        val target: Node,
        var progress: Float,
        val limit: Float
    )

    private val stateFlow: MutableStateFlow<PuzzleSolverData> =
        MutableStateFlow(PuzzleSolverData.PreSolution)
    private val path: MutableList<Node> = mutableListOf()
    private var activeSegment: ActiveSegment? = null

    val state: StateFlow<PuzzleSolverData> = stateFlow

    fun startTracingLine(panel: Panel, start: Node): Graph<Node>? {
        if (!panel.graph.nodes().contains(start)) return null
        path.clear()
        path.add(start)
        activeSegment = null
        stateFlow.value = PuzzleSolverData.InSolution
        return buildLine()
    }

    fun move(panel: Panel, deltaX: Float, deltaY: Float): Graph<Node>? {
        if (!isSolving || path.isEmpty()) return null

        var remainingX = deltaX
        var remainingY = deltaY
        repeat(MAX_WAYPOINTS_PER_MOVE) {
            if (remainingX * remainingX + remainingY * remainingY <= MOVEMENT_EPSILON_SQUARED) {
                return buildLine()
            }

            val current: Node = path.last()
            var segment: ActiveSegment? = activeSegment
            if (segment == null) {
                segment = chooseSegment(panel, current, remainingX, remainingY)
                    ?: return buildLine()
                activeSegment = segment
            }

            val edgeX: Float = segment.target.x - current.x
            val edgeY: Float = segment.target.y - current.y
            val edgeLength: Float = sqrt(edgeX * edgeX + edgeY * edgeY)
            if (edgeLength <= MOVEMENT_EPSILON) {
                activeSegment = null
                return@repeat
            }
            val directionX: Float = edgeX / edgeLength
            val directionY: Float = edgeY / edgeLength
            val movementAlongEdge: Float = remainingX * directionX + remainingY * directionY
            val nextProgress: Float = segment.progress + movementAlongEdge

            when {
                nextProgress <= 0f -> {
                    remainingX += directionX * segment.progress
                    remainingY += directionY * segment.progress
                    activeSegment = null
                }

                nextProgress < segment.limit -> {
                    segment.progress = nextProgress
                    return buildLine()
                }

                segment.limit < edgeLength - MOVEMENT_EPSILON -> {
                    segment.progress = segment.limit
                    return buildLine()
                }

                else -> {
                    val distanceToTarget: Float = edgeLength - segment.progress
                    remainingX -= directionX * distanceToTarget
                    remainingY -= directionY * distanceToTarget
                    arriveAt(segment.target)
                    activeSegment = null
                }
            }
        }
        return buildLine()
    }

    fun stopTrace() {
        stateFlow.value = PuzzleSolverData.PreSolution
        path.clear()
        activeSegment = null
    }

    val isSolving: Boolean get() = stateFlow.value == PuzzleSolverData.InSolution

    fun tracingTip(): Node? {
        val current: Node = path.lastOrNull() ?: return null
        val segment: ActiveSegment = activeSegment
            ?: return Node(current.x, current.y, Modifier.END)
        val edgeX: Float = segment.target.x - current.x
        val edgeY: Float = segment.target.y - current.y
        val edgeLength: Float = sqrt(edgeX * edgeX + edgeY * edgeY)
        val progressRatio: Float =
            if (edgeLength <= MOVEMENT_EPSILON) 0f
            else segment.progress / edgeLength
        return Node(
            current.x + edgeX * progressRatio,
            current.y + edgeY * progressRatio,
            Modifier.END
        )
    }

    private fun chooseSegment(
        panel: Panel,
        current: Node,
        deltaX: Float,
        deltaY: Float
    ): ActiveSegment? {
        val previous: Node? = path.getOrNull(path.lastIndex - 1)
        val movementLength: Float = sqrt(deltaX * deltaX + deltaY * deltaY)
        if (movementLength <= MOVEMENT_EPSILON) return null

        return panel.graph.adjacentNodes(current)
            .asSequence()
            .map { candidate ->
                val edgeX: Float = candidate.x - current.x
                val edgeY: Float = candidate.y - current.y
                val edgeLength: Float = sqrt(edgeX * edgeX + edgeY * edgeY)
                val alignment: Float =
                    if (edgeLength <= MOVEMENT_EPSILON) -1f
                    else (deltaX * edgeX + deltaY * edgeY) / (movementLength * edgeLength)
                val limit: Float =
                    if (candidate == previous) edgeLength
                    else traceLimit(current, candidate, edgeLength)
                Triple(candidate, alignment, limit)
            }
            .filter { (_, alignment, limit) ->
                alignment >= MIN_EDGE_ALIGNMENT && limit > MOVEMENT_EPSILON
            }
            .maxByOrNull { (_, alignment) -> alignment }
            ?.let { (target, _, limit) -> ActiveSegment(target, 0f, limit) }
    }

    private fun traceLimit(from: Node, to: Node, edgeLength: Float): Float {
        val tracedSegments: List<Pair<Node, Node>> = path.zipWithNext()
        if (tracedSegments.any { (start, end) -> sameSegment(from, to, start, end) }) {
            return 0f
        }
        var collisionParameter: Float? = if (to in path) 1f else null
        tracedSegments.forEach { (start, end) ->
            val intersection: Float = intersectionParameter(from, to, start, end)
                ?: return@forEach
            collisionParameter = minOf(collisionParameter ?: intersection, intersection)
        }
        val collisionDistance: Float = (collisionParameter ?: 1f) * edgeLength
        return if (collisionParameter == null) {
            edgeLength
        } else {
            (collisionDistance - SELF_COLLISION_CLEARANCE).coerceAtLeast(0f)
        }
    }

    private fun sameSegment(a: Node, b: Node, c: Node, d: Node): Boolean =
        (a == c && b == d) || (a == d && b == c)

    private fun intersectionParameter(a: Node, b: Node, c: Node, d: Node): Float? {
        val rayX: Float = b.x - a.x
        val rayY: Float = b.y - a.y
        val segmentX: Float = d.x - c.x
        val segmentY: Float = d.y - c.y
        val denominator: Float = cross(rayX, rayY, segmentX, segmentY)
        if (kotlin.math.abs(denominator) <= INTERSECTION_EPSILON) return null

        val offsetX: Float = c.x - a.x
        val offsetY: Float = c.y - a.y
        val rayParameter: Float = cross(offsetX, offsetY, segmentX, segmentY) / denominator
        val segmentParameter: Float = cross(offsetX, offsetY, rayX, rayY) / denominator
        if (rayParameter <= INTERSECTION_EPSILON || rayParameter > 1f + INTERSECTION_EPSILON) {
            return null
        }
        if (segmentParameter < -INTERSECTION_EPSILON ||
            segmentParameter > 1f + INTERSECTION_EPSILON
        ) {
            return null
        }
        return rayParameter.coerceIn(0f, 1f)
    }

    private fun cross(aX: Float, aY: Float, bX: Float, bY: Float): Float =
        aX * bY - aY * bX

    private fun arriveAt(target: Node) {
        val previous: Node? = path.getOrNull(path.lastIndex - 1)
        if (target == previous) {
            path.removeAt(path.lastIndex)
        } else {
            path.add(target)
        }
    }

    private fun buildLine(): Graph<Node> {
        val segment: ActiveSegment? = activeSegment
        val previous: Node? = path.getOrNull(path.lastIndex - 1)
        val isBacktracking: Boolean = segment != null && segment.target == previous
        val renderedPath: List<Node> =
            if (isBacktracking) path.dropLast(1)
            else path
        val graph: MutableGraph<Node> = mutableGraph()
        renderedPath.forEach(graph::addNode)
        renderedPath.zipWithNext().forEach { (from, to) -> graph.putEdge(from, to) }

        val current: Node = renderedPath.lastOrNull() ?: return graph
        if (segment == null && current.modifier == Modifier.END) return graph
        val end: Node = requireNotNull(tracingTip())
        graph.addNode(end)
        graph.putEdge(current, end)
        return graph
    }

    private companion object {
        const val MAX_WAYPOINTS_PER_MOVE = 16
        const val MIN_EDGE_ALIGNMENT = 0.25f
        const val MOVEMENT_EPSILON = 0.0001f
        const val MOVEMENT_EPSILON_SQUARED = MOVEMENT_EPSILON * MOVEMENT_EPSILON
        const val INTERSECTION_EPSILON = 0.0001f
        const val SELF_COLLISION_CLEARANCE = 0.25f
    }
}
