package com.xfastgames.witness.blocks.redstone

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/** Cables as integers on a line: `n` joins `n - 1` and `n + 1` while both are in the run. */
class CableNetworkTests {

    private fun line(length: Int): (Int) -> List<Int> = { n ->
        listOf(n - 1, n + 1).filter { it in 0 until length }
    }

    @Test
    fun `A run touching a source is lit end to end`() {
        val walk = walkCables(start = 5, neighbours = line(10), isSource = { it == 0 })

        assertThat(walk.component).containsExactlyElementsIn(0 until 10)
        assertThat(walk.lit).containsExactlyElementsIn(0 until 10)
    }

    @Test
    fun `A run with no source is dark`() {
        val walk = walkCables(start = 5, neighbours = line(10), isSource = { false })

        assertThat(walk.component).hasSize(10)
        assertThat(walk.lit).isEmpty()
    }

    @Test
    fun `Power stops after the maximum distance from the nearest source`() {
        val walk = walkCables(start = 0, neighbours = line(100), isSource = { it == 0 }, maxDistance = 64)

        assertThat(walk.lit).containsExactlyElementsIn(0..64)
        assertThat(walk.component).hasSize(100)
    }

    @Test
    fun `Two sources light the union of their reaches`() {
        val walk = walkCables(start = 50, neighbours = line(100), isSource = { it == 0 || it == 99 }, maxDistance = 30)

        assertThat(walk.lit).containsExactlyElementsIn((0..30) + (69..99))
    }

    @Test
    fun `A cut run only reaches its own side`() {
        // 4 is missing, so 0..3 and 5..9 are separate runs.
        val neighbours: (Int) -> List<Int> = { n -> line(10)(n).filter { it != 4 } }

        val far = walkCables(start = 7, neighbours = neighbours, isSource = { it == 0 })
        assertThat(far.component).containsExactlyElementsIn(5..9)
        assertThat(far.lit).isEmpty()
    }

    @Test
    fun `The walk gives up on a run bigger than the visit cap`() {
        val walk = walkCables(start = 0, neighbours = line(10_000), isSource = { it == 0 }, maxVisited = 512)

        assertThat(walk.component).hasSize(512)
    }
}
