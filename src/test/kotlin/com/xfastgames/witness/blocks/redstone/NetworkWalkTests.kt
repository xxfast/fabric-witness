package com.xfastgames.witness.blocks.redstone

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * A network as integers in a row: `n` links to `n - 1` and `n + 1` while both are in the row.
 * Which integers are frames, and how they feed, is set per test; everything else is a cable.
 */
class NetworkWalkTests {

    private fun row(length: Int): (Int) -> List<Int> = { n ->
        listOf(n - 1, n + 1).filter { it in 0 until length }
    }

    private val always: (Int) -> Boolean = { true }
    private val never: (Int) -> Boolean = { false }

    /** Frames feed every neighbour when solved (the one-end rule); cables feed everything. */
    private fun feeds(frames: Set<Int>, solved: (Int) -> Boolean): (Int, Int) -> Boolean =
        { from, _ -> from !in frames || solved(from) }

    private fun decays(frames: Set<Int>): (Int, Int) -> Boolean = { from, to -> from !in frames && to !in frames }

    @Test
    fun `A solved chain is powered from its head to its first unsolved frame`() {
        val frames: Set<Int> = (0 until 5).toSet()
        val walk = walkNetwork(
            start = 3,
            links = row(5),
            feeds = feeds(frames, solved = { it in 0..2 }),
            isSource = { it == 0 },
            decays = decays(frames),
        )

        assertThat(walk.component).containsExactlyElementsIn(0 until 5)
        // 0, 1, 2 are solved so 3 is fed and On; 3 is unsolved, so On is not contagious past it.
        assertThat(walk.powered.keys).containsExactlyElementsIn(0..3)
    }

    @Test
    fun `A row of one-end frames goes dark when the source is cut, however they feed each other`() {
        val frames: Set<Int> = (0 until 5).toSet()
        val walk = walkNetwork(
            start = 2,
            links = row(5),
            feeds = feeds(frames, solved = always),
            isSource = never,
            decays = decays(frames),
        )

        assertThat(walk.powered).isEmpty()
    }

    @Test
    fun `A frame with a choice of ends feeds only the side its used nub points at`() {
        val walk = walkNetwork(
            start = 0,
            links = row(5),
            feeds = { from, to -> to == from + 1 },
            isSource = { it == 2 },
            decays = { _, _ -> false },
        )

        assertThat(walk.powered.keys).containsExactly(2, 3, 4)
    }

    @Test
    fun `A frame without a panel is never powered and passes nothing on`() {
        val frames: Set<Int> = (0 until 4).toSet()
        val walk = walkNetwork(
            start = 0,
            links = row(4),
            feeds = feeds(frames, solved = always),
            isSource = { it == 0 },
            canHold = { it != 1 },
            decays = decays(frames),
        )

        assertThat(walk.powered.keys).containsExactly(0)
    }

    @Test
    fun `A ring of cables and frames does not hold itself up`() {
        val ring: (Int) -> List<Int> = { n -> listOf((n + 3) % 4, (n + 1) % 4) }
        val frames: Set<Int> = setOf(0, 2)
        val walk = walkNetwork(
            start = 0,
            links = ring,
            feeds = feeds(frames, solved = always),
            isSource = never,
            decays = decays(frames),
        )

        assertThat(walk.component).containsExactly(0, 1, 2, 3)
        assertThat(walk.powered).isEmpty()
    }

    @Test
    fun `A cable lit through a frame carries that frame as its origin`() {
        // Cable 0 is the source, frame 3 is solved, cables 4 and 5 are lit through it.
        val frames: Set<Int> = setOf(3)
        val walk = walkNetwork(
            start = 0,
            links = row(6),
            feeds = feeds(frames, solved = always),
            isSource = { it == 0 },
            decays = decays(frames),
        )

        assertThat(walk.powered).containsExactly(0, 0, 1, 0, 2, 0, 3, 2, 4, 3, 5, 3)
    }

    @Test
    fun `The distance cap counts cables only and resets at a frame`() {
        // 129 cables straight: the last is dark. 128 cables, a frame, 128 more: all lit.
        val straight = walkNetwork(
            start = 0,
            links = row(130),
            feeds = { _, _ -> true },
            isSource = { it == 0 },
            decays = { _, _ -> true },
        )
        val frames: Set<Int> = setOf(128)
        val relayed = walkNetwork(
            start = 0,
            links = row(257),
            feeds = feeds(frames, solved = always),
            isSource = { it == 0 },
            decays = decays(frames),
        )

        assertThat(straight.powered.keys).containsExactlyElementsIn(0..128)
        assertThat(relayed.powered.keys).containsExactlyElementsIn(0..256)
    }

    @Test
    fun `Two sources at the same distance settle by order, not by where the walk started`() {
        val fromLeft = walkNetwork(
            start = 0,
            links = row(5),
            feeds = { _, _ -> true },
            isSource = { it == 0 || it == 4 },
            decays = { _, _ -> true },
            order = compareBy { it },
        )
        val fromRight = walkNetwork(
            start = 4,
            links = row(5),
            feeds = { _, _ -> true },
            isSource = { it == 0 || it == 4 },
            decays = { _, _ -> true },
            order = compareBy { it },
        )

        assertThat(fromLeft.powered[2]).isEqualTo(0)
        assertThat(fromRight.powered[2]).isEqualTo(0)
    }

    @Test
    fun `The walk stops looking after the visited cap`() {
        val walk = walkNetwork(
            start = 0,
            links = row(100),
            feeds = { _, _ -> true },
            isSource = { it == 0 },
            decays = { _, _ -> false },
            maxVisited = 10,
        )

        assertThat(walk.component).hasSize(10)
        assertThat(walk.powered).hasSize(10)
    }
}
