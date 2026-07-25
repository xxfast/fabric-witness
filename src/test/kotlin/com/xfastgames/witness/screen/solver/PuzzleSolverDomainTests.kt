package com.xfastgames.witness.screen.solver

import com.google.common.graph.Graph
import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.items.data.Edge
import com.xfastgames.witness.items.data.Modifier
import com.xfastgames.witness.items.data.Node
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.screens.solver.PuzzleSolverData
import com.xfastgames.witness.screens.solver.PuzzleSolverDomain
import com.xfastgames.witness.utils.guava.emptyGraph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import net.minecraft.util.DyeColor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("UnstableApiUsage")
@FlowPreview
@ExperimentalCoroutinesApi
class PuzzleSolverDomainTests {

    private val start = Node(0f, 0f, Modifier.START)
    private val corner = Node(1f, 0f)
    private val finish = Node(1f, 1f, Modifier.END)
    private val panel: Panel = Panel.Grid(
        line = emptyGraph(),
        graph = MutableValueGraphBuilder().apply {
            putEdgeValue(start, corner, Modifier.NORMAL)
            putEdgeValue(corner, finish, Modifier.NORMAL)
        },
        backgroundColor = DyeColor.WHITE,
        width = 1,
        height = 1
    )
    private lateinit var domain: PuzzleSolverDomain

    @BeforeEach
    fun setUp() {
        domain = PuzzleSolverDomain()
    }

    @Test
    fun `Starts a trace at the selected node`() {
        val line: Graph<Node> = requireNotNull(domain.startTracingLine(panel, start))

        assertThat(domain.state.value).isInstanceOf(PuzzleSolverData.InSolution::class.java)
        assertThat(line.nodes().filterNot { it.modifier == Modifier.END }).containsExactly(start)
        assertThat(line.end()).isEqualTo(Node(0f, 0f, Modifier.END))
    }

    @Test
    fun `Consumes movement through a waypoint and turns onto the next edge`() {
        domain.startTracingLine(panel, start)

        val firstSegment: Graph<Node> = requireNotNull(domain.move(panel, 0.5f, 0.2f))
        assertThat(firstSegment.end()).isEqualTo(Node(0.5f, 0f, Modifier.END))

        val aroundCorner: Graph<Node> = requireNotNull(domain.move(panel, 0.6f, 0.5f))
        assertThat(aroundCorner.nodes().filterNot { it.modifier == Modifier.END })
            .containsExactly(start, corner)
        assertThat(aroundCorner.end()).isEqualTo(Node(1f, 0.5f, Modifier.END))
    }

    @Test
    fun `Reverses through the previous waypoint without a sticky region`() {
        domain.startTracingLine(panel, start)
        domain.move(panel, 1f, 0f)

        val retracting: Graph<Node> = requireNotNull(domain.move(panel, -0.5f, 0f))
        assertThat(retracting.nodes().filterNot { it.modifier == Modifier.END }).containsExactly(start)
        assertThat(retracting.end()).isEqualTo(Node(0.5f, 0f, Modifier.END))
        assertThat(retracting.edges()).hasSize(1)

        val retracted: Graph<Node> = requireNotNull(domain.move(panel, -0.5f, 0f))
        assertThat(retracted.nodes().filterNot { it.modifier == Modifier.END }).containsExactly(start)
        assertThat(retracted.end()).isEqualTo(Node(0f, 0f, Modifier.END))
    }

    @Test
    fun `Reaches a puzzle end without creating a duplicate live endpoint`() {
        domain.startTracingLine(panel, start)
        domain.move(panel, 1f, 0f)

        val completed: Graph<Node> = requireNotNull(domain.move(panel, 0f, 1f))

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
        domain.startTracingLine(loopPanel, start)
        domain.move(loopPanel, 1f, 0f)
        domain.move(loopPanel, 0f, 1f)
        domain.move(loopPanel, -1f, 0f)

        val blockedBeforeLoop: Graph<Node> = requireNotNull(domain.move(loopPanel, 0f, -1f))

        assertThat(blockedBeforeLoop.edges()).hasSize(4)
        assertThat(blockedBeforeLoop.hasEdgeConnecting(topLeft, start)).isFalse()
        assertThat(domain.tracingTip()).isEqualTo(Node(0f, 0.25f, Modifier.END))

        val stillBlocked: Graph<Node> =
            requireNotNull(domain.move(loopPanel, 0f, -1f))
        assertThat(stillBlocked.edges()).hasSize(4)
        assertThat(domain.tracingTip()).isEqualTo(Node(0f, 0.25f, Modifier.END))
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
        domain.startTracingLine(crossingPanel, start)
        domain.move(crossingPanel, 1f, 1f)
        domain.move(crossingPanel, -1f, 0f)

        val blocked: Graph<Node> = requireNotNull(domain.move(crossingPanel, 1f, -1f))

        assertThat(blocked.nodes()).doesNotContain(crossingEnd)
        val tip: Node = requireNotNull(domain.tracingTip())
        val distanceToCrossing: Float = kotlin.math.hypot(tip.x - 0.5f, tip.y - 0.5f)
        assertThat(distanceToCrossing).isWithin(0.0001f).of(0.25f)
    }

    private fun Graph<Node>.end(): Node? = nodes().firstOrNull { it.modifier == Modifier.END }

    private fun panelOf(vararg edges: Pair<Node, Node>): Panel {
        val graph: MutableValueGraph<Node, Edge> = MutableValueGraphBuilder()
        edges.forEach { (from, to) -> graph.putEdgeValue(from, to, Modifier.NORMAL) }
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
