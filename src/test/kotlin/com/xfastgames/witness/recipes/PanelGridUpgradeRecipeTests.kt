package com.xfastgames.witness.recipes

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PanelGridUpgradeRecipeTests {

    @Test
    fun `all legacy grid upgrade layouts select their original target dimensions`() {
        val cases = listOf(
            Case(2, 2, 1, 1, 2, 2 to 3),
            Case(2, 2, 1, 2, 1, 3 to 2),
            Case(2, 3, 1, 1, 2, 2 to 4),
            Case(2, 3, 1, 2, 1, 3 to 3),
            Case(3, 2, 1, 1, 2, 3 to 3),
            Case(3, 2, 1, 2, 1, 4 to 2),
            Case(2, 4, 1, 2, 1, 3 to 4),
            Case(4, 2, 1, 1, 2, 4 to 3),
            Case(3, 3, 1, 1, 2, 3 to 4),
            Case(3, 3, 1, 2, 1, 4 to 3),
            Case(3, 3, 3, 2, 2, 4 to 4),
            Case(3, 4, 1, 2, 1, 4 to 4),
            Case(4, 3, 1, 1, 2, 4 to 4),
            Case(2, 2, 8, 3, 3, 4 to 4, 1, 1),
        )

        cases.forEach { case -> assertThat(target(case)).isEqualTo(case.target) }
    }

    @Test
    fun `expansion continues past the old 4x4 ceiling`() {
        // One tablet adds one row or column, whatever the source size.
        assertThat(target(Case(4, 4, 1, 2, 1, 5 to 4))).isEqualTo(5 to 4)
        assertThat(target(Case(4, 4, 1, 1, 2, 4 to 5))).isEqualTo(4 to 5)
        // A full ring adds two on each axis.
        assertThat(target(Case(6, 6, 8, 3, 3, 8 to 8, 1, 1))).isEqualTo(8 to 8)
        // Source position no longer gates the ring, it only decides which sides grow.
        assertThat(target(Case(6, 6, 8, 3, 3, 8 to 8, 0, 0))).isEqualTo(8 to 8)
    }

    @Test
    fun `the size cap is a hard stop on either axis`() {
        assertThat(target(Case(8, 8, 1, 2, 1, 9 to 8))).isEqualTo(9 to 8)
        assertThat(target(Case(9, 9, 1, 2, 1, 9 to 9))).isNull()
        assertThat(target(Case(9, 2, 1, 2, 1, 9 to 2))).isNull()
        assertThat(target(Case(2, 9, 1, 1, 2, 2 to 9))).isNull()
        assertThat(target(Case(8, 8, 8, 3, 3, 8 to 8, 1, 1))).isNull()
    }

    /**
     * The panel grows on the side the tablets are on. Both axes are mirrored between the crafting
     * grid and node indices, so an offset of 0 means the source stays at the high end of the axis.
     */
    @Test
    fun `the anchor grows the panel towards the tablets`() {
        // Tablet to the right of the panel, so the panel is at footprint position 0: the new column
        // is inserted before the source in index order, which renders on the tablet's side.
        assertThat(PanelGridUpgradeLayouts.anchorOffset(footprint = 2, sourcePosition = 0)).isEqualTo(1)

        // Tablet to the left, panel at position 1: the new column goes after the source instead.
        assertThat(PanelGridUpgradeLayouts.anchorOffset(footprint = 2, sourcePosition = 1)).isEqualTo(0)

        // Tablet above the panel, so the panel is the lower of the two rows, position 1: the new row
        // lands on top and the old content stays on the bottom.
        assertThat(PanelGridUpgradeLayouts.anchorOffset(footprint = 2, sourcePosition = 1)).isEqualTo(0)

        // Centred in a ring: one new row or column on each side.
        assertThat(PanelGridUpgradeLayouts.anchorOffset(footprint = 3, sourcePosition = 1)).isEqualTo(1)
    }

    @Test
    fun `layout maths rejects gaps, strays and lone panels`() {
        // Unfilled bounding box.
        assertThat(PanelGridUpgradeLayouts.target(2, 2, 1, 2, 2, 0, 0, isFilledRectangle = false)).isNull()
        // Tablet count that doesn't fill the footprint around the source.
        assertThat(target(Case(2, 3, 1, 2, 2, 3 to 4))).isNull()
        // A lone panel is the recycle recipe's business, not ours.
        assertThat(target(Case(3, 3, 0, 1, 1, 3 to 3))).isNull()
    }

    private fun target(case: Case): Pair<Int, Int>? = PanelGridUpgradeLayouts.target(
        case.sourceWidth,
        case.sourceHeight,
        case.tabletCount,
        case.layoutWidth,
        case.layoutHeight,
        case.sourceX,
        case.sourceY,
        isFilledRectangle = true,
    )

    private data class Case(
        val sourceWidth: Int,
        val sourceHeight: Int,
        val tabletCount: Int,
        val layoutWidth: Int,
        val layoutHeight: Int,
        val target: Pair<Int, Int>,
        val sourceX: Int = 0,
        val sourceY: Int = 0,
    )
}
