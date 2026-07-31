package com.xfastgames.witness.screen.solver

import com.google.common.graph.Graph
import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.items.data.Edge
import com.xfastgames.witness.items.data.Modifier
import com.xfastgames.witness.items.data.Node
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.Symbol
import com.xfastgames.witness.screens.solver.PuzzleSolverData
import com.xfastgames.witness.screens.solver.PuzzleSolver
import com.xfastgames.witness.utils.guava.emptyGraph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import net.minecraft.util.DyeColor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("UnstableApiUsage")
@FlowPreview
@ExperimentalCoroutinesApi
class PuzzleSolverTests {

    private val start = Node(0f, 0f, Modifier.START)
    private val corner = Node(1f, 0f)
    private val finish = Node(1f, 1f, Modifier.END)
    private val panel: Panel = Panel.Grid(
        line = emptyGraph(),
        graph = MutableValueGraphBuilder().apply {
            putEdgeValue(start, corner, Edge.NORMAL)
            putEdgeValue(corner, finish, Edge.NORMAL)
        },
        backgroundColor = DyeColor.WHITE,
        width = 1,
        height = 1
    )
    private lateinit var solver: PuzzleSolver

    @BeforeEach
    fun setUp() {
        solver = PuzzleSolver()
    }

    @Test
    fun `Starts a trace at the selected node`() {
        val line: Graph<Node> = requireNotNull(solver.startTracingLine(panel, start))

        assertThat(solver.state.value).isInstanceOf(PuzzleSolverData.InSolution::class.java)
        assertThat(line.nodes().filterNot { it.modifier == Modifier.END }).containsExactly(start)
        assertThat(line.end()).isEqualTo(Node(0f, 0f, Modifier.END))
    }

    @Test
    fun `Consumes movement through a waypoint and turns onto the next edge`() {
        solver.startTracingLine(panel, start)

        val firstSegment: Graph<Node> = requireNotNull(solver.move(panel, 0.5f, 0.2f))
        assertThat(firstSegment.end()).isEqualTo(Node(0.5f, 0f, Modifier.END))

        val aroundCorner: Graph<Node> = requireNotNull(solver.move(panel, 0.6f, 0.5f))
        assertThat(aroundCorner.nodes().filterNot { it.modifier == Modifier.END })
            .containsExactly(start, corner)
        assertThat(aroundCorner.end()).isEqualTo(Node(1f, 0.5f, Modifier.END))
    }

    @Test
    fun `Reverses through the previous waypoint without a sticky region`() {
        solver.startTracingLine(panel, start)
        solver.move(panel, 1f, 0f)

        val retracting: Graph<Node> = requireNotNull(solver.move(panel, -0.5f, 0f))
        assertThat(retracting.nodes().filterNot { it.modifier == Modifier.END }).containsExactly(start)
        assertThat(retracting.end()).isEqualTo(Node(0.5f, 0f, Modifier.END))
        assertThat(retracting.edges()).hasSize(1)

        val retracted: Graph<Node> = requireNotNull(solver.move(panel, -0.5f, 0f))
        assertThat(retracted.nodes().filterNot { it.modifier == Modifier.END }).containsExactly(start)
        assertThat(retracted.end()).isEqualTo(Node(0f, 0f, Modifier.END))
    }

    @Test
    fun `Reaches a puzzle end without creating a duplicate live endpoint`() {
        solver.startTracingLine(panel, start)
        solver.move(panel, 1f, 0f)

        val completed: Graph<Node> = requireNotNull(solver.move(panel, 0f, 1f))

        assertThat(completed.nodes()).containsExactly(start, corner, finish)
        assertThat(completed.edges()).hasSize(2)
    }

    @Test
    fun `Stops one line width before a visited waypoint on an unused segment`() {
        val bottomRight = Node(1f, 0f)
        val topRight = Node(1f, 1f)
        val topLeft = Node(0f, 1f)
        val loopPanel: Panel = panelOf(
            start to bottomRight,
            bottomRight to topRight,
            topRight to topLeft,
            topLeft to start
        )
        solver.startTracingLine(loopPanel, start)
        solver.move(loopPanel, 1f, 0f)
        solver.move(loopPanel, 0f, 1f)
        solver.move(loopPanel, -1f, 0f)

        val blockedBeforeLoop: Graph<Node> = requireNotNull(solver.move(loopPanel, 0f, -1f))

        assertThat(blockedBeforeLoop.edges()).hasSize(4)
        assertThat(blockedBeforeLoop.hasEdgeConnecting(topLeft, start)).isFalse()
        assertThat(solver.tracingTip()).isEqualTo(Node(0f, 0.25f, Modifier.END))

        val stillBlocked: Graph<Node> =
            requireNotNull(solver.move(loopPanel, 0f, -1f))
        assertThat(stillBlocked.edges()).hasSize(4)
        assertThat(solver.tracingTip()).isEqualTo(Node(0f, 0.25f, Modifier.END))
    }

    @Test
    fun `Cannot trace a segment that crosses the existing line`() {
        val diagonalEnd = Node(1f, 1f)
        val topLeft = Node(0f, 1f)
        val crossingEnd = Node(1f, 0f)
        val crossingPanel: Panel = panelOf(
            start to diagonalEnd,
            diagonalEnd to topLeft,
            topLeft to crossingEnd
        )
        solver.startTracingLine(crossingPanel, start)
        solver.move(crossingPanel, 1f, 1f)
        solver.move(crossingPanel, -1f, 0f)

        val blocked: Graph<Node> = requireNotNull(solver.move(crossingPanel, 1f, -1f))

        assertThat(blocked.nodes()).doesNotContain(crossingEnd)
        val tip: Node = requireNotNull(solver.tracingTip())
        val distanceToCrossing: Float = kotlin.math.hypot(tip.x - 0.5f, tip.y - 0.5f)
        assertThat(distanceToCrossing).isWithin(0.0001f).of(0.25f)
    }

    @Test
    fun `Refuses to start a trace on a node that is not a start point`() {
        assertThat(solver.startTracingLine(panel, corner)).isNull()
        assertThat(solver.state.value).isInstanceOf(PuzzleSolverData.PreSolution::class.java)
    }

    @Test
    fun `Traces into a broken edge up to the gap but never across it`() {
        val brokenPanel: Panel = Panel.Grid(
            line = emptyGraph(),
            graph = MutableValueGraphBuilder().apply {
                putEdgeValue(start, corner, Edge.BREAK)
            },
            backgroundColor = DyeColor.WHITE,
            width = 1,
            height = 1
        )
        solver.startTracingLine(brokenPanel, start)

        val stub: Graph<Node> = requireNotNull(solver.move(brokenPanel, 1f, 0f))

        // Half the edge, less one line thickness for the gap and the line's round tip.
        assertThat(stub.nodes()).doesNotContain(corner)
        assertThat(solver.tracingTip()).isEqualTo(Node(0.25f, 0f, Modifier.END))

        solver.move(brokenPanel, 1f, 0f)
        assertThat(solver.tracingTip()).isEqualTo(Node(0.25f, 0f, Modifier.END))
    }

    @Test
    fun `Backs out of a broken edge stub`() {
        val brokenPanel: Panel = panelOf(
            Triple(start, corner, Edge.BREAK),
            Triple(corner, finish, Edge.NORMAL)
        )
        solver.startTracingLine(brokenPanel, start)
        solver.move(brokenPanel, 1f, 0f)
        solver.move(brokenPanel, -1f, 0f)

        assertThat(solver.tracingTip()).isEqualTo(Node(0f, 0f, Modifier.END))
    }

    @Test
    fun `Accepts a path that reaches an end point`() {
        solver.startTracingLine(panel, start)
        solver.move(panel, 1f, 0f)
        solver.move(panel, 0f, 1f)

        val submitted: Graph<Node> = requireNotNull(solver.submit(panel))

        assertThat(solver.state.value).isInstanceOf(PuzzleSolverData.SolutionAccepted::class.java)
        assertThat(submitted.nodes()).containsExactly(start, corner, finish)
    }

    @Test
    fun `Accepts a line that has committed onto the end point without reaching it`() {
        solver.startTracingLine(panel, start)
        solver.move(panel, 1f, 0f)
        solver.move(panel, 0f, 0.5f)

        val submitted: Graph<Node> = requireNotNull(solver.submit(panel))

        assertThat(solver.state.value).isInstanceOf(PuzzleSolverData.SolutionAccepted::class.java)
        assertThat(submitted.nodes()).containsExactly(start, corner, finish)
    }

    @Test
    fun `Aborts a line that has backed out of the end point`() {
        solver.startTracingLine(panel, start)
        solver.move(panel, 1f, 0f)
        solver.move(panel, 0f, 1f)
        solver.move(panel, 0f, -0.5f)

        val submitted: Graph<Node> = requireNotNull(solver.submit(panel))

        assertThat(solver.state.value).isInstanceOf(PuzzleSolverData.SolutionAborted::class.java)
        assertThat(submitted.nodes()).isEmpty()
    }

    @Test
    fun `Aborts a path that stops short of an end point`() {
        solver.startTracingLine(panel, start)
        solver.move(panel, 0.5f, 0f)

        val submitted: Graph<Node> = requireNotNull(solver.submit(panel))

        assertThat(solver.state.value).isInstanceOf(PuzzleSolverData.SolutionAborted::class.java)
        assertThat(submitted.nodes()).isEmpty()
    }

    @Test
    fun `Aborts a path released on a plain node rather than rejecting it`() {
        // Nothing was submitted, so the panel never gets to fail it.
        solver.startTracingLine(panel, start)
        solver.move(panel, 1f, 0f)

        val submitted: Graph<Node> = requireNotNull(solver.submit(panel))

        assertThat(solver.state.value).isInstanceOf(PuzzleSolverData.SolutionAborted::class.java)
        assertThat(submitted.nodes()).isEmpty()
    }

    @Test
    fun `Reports reaching the end point, and leaving it again`() {
        solver.startTracingLine(panel, start)
        solver.move(panel, 1f, 0f)
        assertThat(solver.isAtFinish).isFalse()

        solver.move(panel, 0f, 1f)
        assertThat(solver.isAtFinish).isTrue()

        // Retracting down the nub still counts as reached; only leaving the node undoes it.
        solver.move(panel, 0f, -0.5f)
        assertThat(solver.isAtFinish).isTrue()

        solver.move(panel, 0f, -0.5f)
        assertThat(solver.isAtFinish).isFalse()
    }

    @Test
    fun `Rejects a path that reaches the end but misses a hexagon`() {
        // The hexagon sits on the far side of a detour the short path never takes
        // (rules/witness/04-hexagon-dots.md).
        val detour = Node(0f, 1f, symbol = Symbol.HEXAGON)
        val hexagonPanel: Panel = panelOf(
            start to corner,
            corner to finish,
            start to detour,
            detour to finish
        )
        solver.startTracingLine(hexagonPanel, start)
        solver.move(hexagonPanel, 1f, 0f)
        solver.move(hexagonPanel, 0f, 1f)

        val submitted: Graph<Node> = requireNotNull(solver.submit(hexagonPanel))

        assertThat(solver.state.value).isInstanceOf(PuzzleSolverData.SolutionRejected::class.java)
        assertThat(submitted.nodes()).isEmpty()
    }

    @Test
    fun `Accepts a path that reaches the end covering every hexagon`() {
        val detour = Node(0f, 1f, symbol = Symbol.HEXAGON)
        val hexagonPanel: Panel = panelOf(
            start to corner,
            corner to finish,
            start to detour,
            detour to finish
        )
        solver.startTracingLine(hexagonPanel, start)
        solver.move(hexagonPanel, 0f, 1f)
        solver.move(hexagonPanel, 1f, 0f)

        val submitted: Graph<Node> = requireNotNull(solver.submit(hexagonPanel))

        assertThat(solver.state.value).isInstanceOf(PuzzleSolverData.SolutionAccepted::class.java)
        assertThat(submitted.nodes()).containsExactly(start, detour, finish)
    }

    @Test
    fun `A dotted edge traces exactly like a plain one`() {
        // A symbol never changes how the line moves; only the traversal state does.
        val dottedPanel: Panel = panelOf(
            Triple(start, corner, Edge(Modifier.NORMAL, Symbol.HEXAGON)),
            Triple(corner, finish, Edge.NORMAL)
        )
        solver.startTracingLine(dottedPanel, start)
        solver.move(dottedPanel, 1f, 0f)
        solver.move(dottedPanel, 0f, 1f)

        val submitted: Graph<Node> = requireNotNull(solver.submit(dottedPanel))

        assertThat(solver.state.value).isInstanceOf(PuzzleSolverData.SolutionAccepted::class.java)
        assertThat(submitted.nodes()).containsExactly(start, corner, finish)
    }

    private fun Graph<Node>.end(): Node? = nodes().firstOrNull { it.modifier == Modifier.END }

    private fun panelOf(vararg edges: Pair<Node, Node>): Panel =
        panelOf(*edges.map { (from, to) -> Triple(from, to, Edge.NORMAL) }.toTypedArray())

    private fun panelOf(vararg edges: Triple<Node, Node, Edge>): Panel {
        val graph: MutableValueGraph<Node, Edge> = MutableValueGraphBuilder()
        edges.forEach { (from, to, modifier) -> graph.putEdgeValue(from, to, modifier) }
        return Panel.Grid(
            line = emptyGraph(),
            graph = graph,
            backgroundColor = DyeColor.WHITE,
            width = 1,
            height = 1
        )
    }

    private fun MutableValueGraphBuilder(): MutableValueGraph<Node, Edge> =
        ValueGraphBuilder.undirected().build()
}
