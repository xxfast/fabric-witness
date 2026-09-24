package com.xfastgames.witness.blocks.redstone

import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.items.data.Side
import org.junit.jupiter.api.Test

/** Where a solved frame's power goes (rules/minecraft/05-puzzle-frame.md, "where the power goes"). */
class FrameRoutingTests {

    private val up: Set<Side> = setOf(Side.TOP)

    @Test
    fun `A one-end panel feeds every side whatever is beside it`() {
        assertThat(outputSides(singleEnd = true, exit = up, neighbours = setOf(Side.RIGHT), input = null))
            .containsExactlyElementsIn(Side.entries)
    }

    @Test
    fun `A choice of ends with one place to go feeds that place, not the nub`() {
        // The Orchard post: tips point up, the cable leaves to the right.
        assertThat(outputSides(singleEnd = false, exit = up, neighbours = setOf(Side.RIGHT), input = null))
            .containsExactly(Side.RIGHT)
    }

    @Test
    fun `The side power came in on is not a place to go`() {
        // A bracketed row of trees, fed from the left: the only other neighbour is the right.
        assertThat(outputSides(singleEnd = false, exit = up, neighbours = setOf(Side.LEFT, Side.RIGHT), input = Side.LEFT))
            .containsExactly(Side.RIGHT)
    }

    @Test
    fun `The only neighbour being the input makes the frame a tail on its nub`() {
        assertThat(outputSides(singleEnd = false, exit = up, neighbours = setOf(Side.LEFT), input = Side.LEFT))
            .containsExactly(Side.TOP)
    }

    @Test
    fun `Two places to go and the nub rules`() {
        assertThat(outputSides(singleEnd = false, exit = up, neighbours = setOf(Side.LEFT, Side.RIGHT), input = null))
            .containsExactly(Side.TOP)
        assertThat(outputSides(singleEnd = false, exit = setOf(Side.LEFT), neighbours = setOf(Side.LEFT, Side.RIGHT), input = null))
            .containsExactly(Side.LEFT)
    }

    @Test
    fun `A single neighbour on the nub side is the same answer either way`() {
        assertThat(outputSides(singleEnd = false, exit = up, neighbours = up, input = null)).containsExactly(Side.TOP)
    }

    @Test
    fun `Nothing beside it and the nub rules, even pointing at air`() {
        assertThat(outputSides(singleEnd = false, exit = up, neighbours = emptySet(), input = null)).containsExactly(Side.TOP)
        assertThat(outputSides(singleEnd = false, exit = emptySet(), neighbours = emptySet(), input = null)).isEmpty()
    }

    @Test
    fun `An input from the back or the stand leaves out nothing`() {
        assertThat(outputSides(singleEnd = false, exit = up, neighbours = setOf(Side.RIGHT), input = null))
            .containsExactly(Side.RIGHT)
    }
}
