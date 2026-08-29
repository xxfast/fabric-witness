package com.xfastgames.witness.items.data

import com.google.common.graph.Graph
import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.utils.guava.emptyGraph
import net.minecraft.world.item.DyeColor
import org.junit.jupiter.api.Test

/** The pure verdict the server re-runs on a submitted path (rules/minecraft/05-puzzle-frame.md). */
@Suppress("UnstableApiUsage")
class SolutionsTests {

    private val start = Node(0f, 0f, Modifier.START)
    private val corner = Node(1f, 0f)
    private val finish = Node(1f, 1f, Modifier.END)
    private val stray = Node(0f, 1f)
    private val panel: Panel = Panel.Grid(
        line = emptyGraph(),
        graph = ValueGraphBuilder.undirected().build<Node, Edge>().apply {
            putEdgeValue(start, corner, Edge.NORMAL)
            putEdgeValue(corner, finish, Edge.NORMAL)
            putEdgeValue(start, stray, Edge.BREAK)
        },
        backgroundColor = DyeColor.WHITE,
        width = 1,
        height = 1
    )

    @Test
    fun `Accepts a start to end path along normal edges`() {
        assertThat(panel.verdict(listOf(start, corner, finish))).isEqualTo(Verdict.Accepted)
    }

    @Test
    fun `Rejects a path that stops short of an end`() {
        assertThat(panel.verdict(listOf(start, corner))).isEqualTo(Verdict.Rejected())
    }

    @Test
    fun `Rejects a path across a broken edge`() {
        assertThat(panel.verdict(listOf(start, stray))).isEqualTo(Verdict.Rejected())
    }

    @Test
    fun `Rejects a path that is not a walk on the grid`() {
        assertThat(panel.verdict(listOf(start, finish))).isEqualTo(Verdict.Rejected())
    }

    @Test
    fun `A path becomes a line joined in order`() {
        val line: Graph<Node> = listOf(start, corner, finish).toLine()

        assertThat(line.nodes()).containsExactly(start, corner, finish)
        assertThat(line.hasEdgeConnecting(start, corner)).isTrue()
        assertThat(line.hasEdgeConnecting(corner, finish)).isTrue()
        assertThat(line.hasEdgeConnecting(start, finish)).isFalse()
    }

    @Test
    fun `A squared off nub exits one side`() {
        val anchor = Node(2f, 1f)
        fun nub(dx: Float, dy: Float) = Node(anchor.x + dx * END_POINT_LENGTH, anchor.y + dy * END_POINT_LENGTH, Modifier.END)

        // Observed in game 2026-08-29: the nub the player sees on the right has dx < 0. Panels are
        // drawn mirrored on x, so do not "fix" these two to match the coordinate signs.
        assertThat(listOf(start, anchor, nub(-1f, 0f)).exitSides()).containsExactly(Side.RIGHT)
        assertThat(listOf(start, anchor, nub(1f, 0f)).exitSides()).containsExactly(Side.LEFT)
        assertThat(listOf(start, anchor, nub(0f, 1f)).exitSides()).containsExactly(Side.TOP)
        assertThat(listOf(start, anchor, nub(0f, -1f)).exitSides()).containsExactly(Side.BOTTOM)
    }

    @Test
    fun `A diagonal corner nub forks to both its sides`() {
        val anchor = Node(2f, 2f)
        val diagonal: Float = END_POINT_LENGTH / kotlin.math.sqrt(2f)
        val nub = Node(anchor.x + diagonal, anchor.y + diagonal, Modifier.END)

        assertThat(listOf(start, anchor, nub).exitSides()).containsExactly(Side.TOP, Side.LEFT)
    }

    @Test
    fun `A path that does not end on a nub exits nowhere`() {
        assertThat(listOf(start, corner).exitSides()).isEmpty()
        assertThat(emptyList<Node>().exitSides()).isEmpty()
    }

    @Test
    fun `withLine keeps everything but the line`() {
        val drawn: Panel = panel.withLine(listOf(start, corner, finish).toLine())

        assertThat(drawn.line.nodes()).hasSize(3)
        assertThat(drawn.graph).isSameInstanceAs(panel.graph)
        assertThat(drawn.withLine(emptyGraph()).line.nodes()).isEmpty()
    }
}
