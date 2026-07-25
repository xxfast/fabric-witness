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

        cases.forEach { case ->
            assertThat(
                PanelGridUpgradeLayouts.target(
                    case.sourceWidth,
                    case.sourceHeight,
                    case.tabletCount,
                    case.layoutWidth,
                    case.layoutHeight,
                    case.sourceX,
                    case.sourceY,
                    isFilledRectangle = true,
                )
            ).isEqualTo(case.target)
        }
        assertThat(PanelGridUpgradeLayouts.displays).hasSize(cases.size)
    }

    @Test
    fun `layout table rejects missing-cost layout gaps and non-legacy expansions`() {
        assertThat(PanelGridUpgradeLayouts.target(2, 2, 1, 2, 2, 0, 0, isFilledRectangle = false)).isNull()
        assertThat(PanelGridUpgradeLayouts.target(2, 3, 1, 1, 2, 0, 0, isFilledRectangle = true)).isEqualTo(2 to 4)
        assertThat(PanelGridUpgradeLayouts.target(2, 3, 1, 2, 2, 0, 0, isFilledRectangle = true)).isNull()
        assertThat(PanelGridUpgradeLayouts.target(2, 2, 8, 3, 3, 0, 0, isFilledRectangle = true)).isNull()
    }

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
