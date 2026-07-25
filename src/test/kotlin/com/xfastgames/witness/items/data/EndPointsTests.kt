package com.xfastgames.witness.items.data

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

@Suppress("UnstableApiUsage")
class EndPointsTests {

    private val panel: Panel = Panel.Grid.ofSize(3)
    private val corner = Node(0.5f, 0.5f)
    private val bottomEdge = Node(1.5f, 0.5f)
    private val middle = Node(1.5f, 1.5f)

    @Test
    fun `Hangs a nub off a border node, pointing out of the panel`() {
        val updated: Panel = requireNotNull(panel.withEndPointToggled(bottomEdge))
        val nub: Node = requireNotNull(updated.endPointOf(bottomEdge))

        assertThat(nub).isEqualTo(Node(1.5f, 0.5f - END_POINT_LENGTH, Modifier.END))
        assertThat(updated.graph.adjacentNodes(nub)).containsExactly(bottomEdge)
        assertThat(updated.graph.edgeValue(bottomEdge, nub).get()).isEqualTo(Modifier.NORMAL)
    }

    @Test
    fun `Points a corner nub diagonally`() {
        val updated: Panel = requireNotNull(panel.withEndPointToggled(corner))
        val nub: Node = requireNotNull(updated.endPointOf(corner))

        val diagonal: Float = END_POINT_LENGTH / sqrt(2f)
        assertThat(nub.x).isWithin(0.0001f).of(0.5f - diagonal)
        assertThat(nub.y).isWithin(0.0001f).of(0.5f - diagonal)
    }

    @Test
    fun `Cycles a corner through diagonal, then both borders, then bare`() {
        val diagonal: Float = END_POINT_LENGTH / sqrt(2f)

        val first: Panel = requireNotNull(panel.withEndPointToggled(corner))
        val diagonalNub: Node = requireNotNull(first.endPointOf(corner))
        assertThat(diagonalNub.x).isWithin(0.0001f).of(0.5f - diagonal)
        assertThat(diagonalNub.y).isWithin(0.0001f).of(0.5f - diagonal)

        val second: Panel = requireNotNull(first.withEndPointToggled(corner))
        assertThat(second.endPointOf(corner)).isEqualTo(Node(0.5f, 0.5f - END_POINT_LENGTH, Modifier.END))

        val third: Panel = requireNotNull(second.withEndPointToggled(corner))
        assertThat(third.endPointOf(corner)).isEqualTo(Node(0.5f - END_POINT_LENGTH, 0.5f, Modifier.END))

        val bare: Panel = requireNotNull(third.withEndPointToggled(corner))
        assertThat(bare.endPointOf(corner)).isNull()
        assertThat(bare.graph.nodes()).isEqualTo(panel.graph.nodes())
    }

    @Test
    fun `Refuses an interior node`() {
        assertThat(panel.withEndPointToggled(middle)).isNull()
    }

    @Test
    fun `Toggles the same border node back to bare`() {
        val added: Panel = requireNotNull(panel.withEndPointToggled(bottomEdge))
        val removed: Panel = requireNotNull(added.withEndPointToggled(bottomEdge))

        assertThat(removed.graph.nodes()).isEqualTo(panel.graph.nodes())
        assertThat(removed.endPointOf(bottomEdge)).isNull()
    }

    @Test
    fun `Removes a nub clicked directly`() {
        val added: Panel = requireNotNull(panel.withEndPointToggled(bottomEdge))
        val nub: Node = requireNotNull(added.endPointOf(bottomEdge))

        val removed: Panel = requireNotNull(added.withEndPointToggled(nub))

        assertThat(removed.graph.nodes()).doesNotContain(nub)
    }

    @Test
    fun `Keeps the border where it was once a nub sits outside it`() {
        val added: Panel = requireNotNull(panel.withEndPointToggled(bottomEdge))

        // The nub is the outermost node now, but it must not count as lattice for the next edit.
        assertThat(added.withEndPointToggled(middle)).isNull()
        assertThat(added.withEndPointToggled(corner)).isNotNull()
    }
}
