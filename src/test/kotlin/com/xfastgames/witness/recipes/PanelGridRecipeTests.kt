package com.xfastgames.witness.recipes

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PanelGridRecipeTests {

    @Test
    fun `all legacy grid layouts select their original target dimensions`() {
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

    /**
     * The nine deleted `puzzle_panel_grid_*.json` base recipes, asserted against the formula that
     * replaced them. A tablet is a 1×1-cell (2×2-node) panel costing 1, so a footprint of pure
     * tablets spends all but one of them growing that seed, and lands on the size the footprint
     * describes. InternalTarget here are the `width`/`height` the old JSON hardcoded.
     */
    @Test
    fun `a footprint of pure tablets builds the panel the old base recipes did`() {
        val cases = listOf(
            // footprint cells, tablets in the grid, target nodes
            Triple(1 to 1, 1, 2 to 2),
            Triple(1 to 2, 2, 2 to 3),
            Triple(2 to 1, 2, 3 to 2),
            Triple(1 to 3, 3, 2 to 4),
            Triple(3 to 1, 3, 4 to 2),
            Triple(2 to 2, 4, 3 to 3),
            Triple(2 to 3, 6, 3 to 4),
            Triple(3 to 2, 6, 4 to 3),
            Triple(3 to 3, 9, 4 to 4),
        )

        cases.forEach { (footprint, tabletsInGrid, target) ->
            val (layoutWidth, layoutHeight) = footprint
            assertThat(
                PanelGridLayouts.target(
                    sourceWidth = 2,
                    sourceHeight = 2,
                    tabletCount = tabletsInGrid - 1,
                    layoutWidth = layoutWidth,
                    layoutHeight = layoutHeight,
                    sourceX = 0,
                    sourceY = 0,
                    isFilledRectangle = true,
                    sourceIsPanel = false,
                )
            ).isEqualTo(target)

            // ...and the seed's own tablet plus what it paid is the old recipe's hardcoded cost.
            assertThat(1 + (tabletsInGrid - 1)).isEqualTo(layoutWidth * layoutHeight)
        }

        assertThat(PanelGridLayouts.seedFootprints).containsExactlyElementsIn(cases.map { it.first })
    }

    @Test
    fun `a seed accepts a lone tablet where a panel would be sent to recycle`() {
        // Same footprint and tablet count, opposite verdicts: a 1x1 footprint paying nothing is the
        // single-tablet craft when it is a seed, and a lone panel awaiting recycle when it is not.
        val seed = PanelGridLayouts.target(2, 2, 0, 1, 1, 0, 0, true, sourceIsPanel = false)
        assertThat(seed).isEqualTo(2 to 2)
        assertThat(PanelGridLayouts.target(2, 2, 0, 1, 1, 0, 0, true)).isNull()
    }

    @Test
    fun `a seed is capped like any other source`() {
        // 3x3 is the largest footprint a crafting grid holds, so a seed can never approach the cap.
        assertThat(PanelGridLayouts.target(2, 2, 8, 3, 3, 0, 0, true, sourceIsPanel = false))
            .isEqualTo(4 to 4)
        // Gaps are rejected on the seed path too.
        assertThat(PanelGridLayouts.target(2, 2, 2, 2, 2, 0, 0, true, sourceIsPanel = false))
            .isNull()
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
        assertThat(target(Case(10, 10, 1, 2, 1, 11 to 10))).isEqualTo(11 to 10)
        assertThat(target(Case(11, 11, 1, 2, 1, 11 to 11))).isNull()
        assertThat(target(Case(11, 2, 1, 2, 1, 11 to 2))).isNull()
        assertThat(target(Case(2, 11, 1, 1, 2, 2 to 11))).isNull()
        assertThat(target(Case(10, 10, 8, 3, 3, 10 to 10, 1, 1))).isNull()
    }

    /**
     * The panel grows on the side the tablets are on. Both axes are mirrored between the crafting
     * grid and node indices, so an offset of 0 means the source stays at the high end of the axis.
     */
    @Test
    fun `the anchor grows the panel towards the tablets`() {
        // Tablet to the right of the panel, so the panel is at footprint position 0: the new column
        // is inserted before the source in index order, which renders on the tablet's side.
        assertThat(PanelGridLayouts.anchorOffset(footprint = 2, sourcePosition = 0)).isEqualTo(1)

        // Tablet to the left, panel at position 1: the new column goes after the source instead.
        assertThat(PanelGridLayouts.anchorOffset(footprint = 2, sourcePosition = 1)).isEqualTo(0)

        // Tablet above the panel, so the panel is the lower of the two rows, position 1: the new row
        // lands on top and the old content stays on the bottom.
        assertThat(PanelGridLayouts.anchorOffset(footprint = 2, sourcePosition = 1)).isEqualTo(0)

        // Centred in a ring: one new row or column on each side.
        assertThat(PanelGridLayouts.anchorOffset(footprint = 3, sourcePosition = 1)).isEqualTo(1)
    }

    @Test
    fun `layout maths rejects gaps, strays and lone panels`() {
        // Unfilled bounding box.
        assertThat(PanelGridLayouts.target(2, 2, 1, 2, 2, 0, 0, isFilledRectangle = false)).isNull()
        // Tablet count that doesn't fill the footprint around the source.
        assertThat(target(Case(2, 3, 1, 2, 2, 3 to 4))).isNull()
        // A lone panel is the recycle recipe's business, not ours.
        assertThat(target(Case(3, 3, 0, 1, 1, 3 to 3))).isNull()
    }

    private fun target(case: Case): Pair<Int, Int>? = PanelGridLayouts.target(
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
