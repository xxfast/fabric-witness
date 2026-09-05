package com.xfastgames.witness.recipes

import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.recipes.PanelTreeLayouts.Kind
import com.xfastgames.witness.recipes.PanelTreeLayouts.Slot
import org.junit.jupiter.api.Test

class PanelTreeRecipeTests {

    /** A column at [x] whose top-to-bottom contents are [kinds]; the last kind lands on row [bottomY]. */
    private fun column(x: Int, bottomY: Int, vararg kinds: Kind): List<Slot> =
        kinds.mapIndexed { index, kind -> Slot(x, bottomY - (kinds.size - 1) + index, kind) }

    @Test
    fun `a sapling with tablets stacked on it seeds a tree of one level per tablet`() {
        assertThat(PanelTreeLayouts.levels(column(0, 2, Kind.TABLET, Kind.SAPLING))).isEqualTo(1)
        assertThat(PanelTreeLayouts.levels(column(0, 2, Kind.TABLET, Kind.TABLET, Kind.SAPLING))).isEqualTo(2)
    }

    @Test
    fun `the column can sit anywhere in the crafting grid`() {
        assertThat(PanelTreeLayouts.levels(column(2, 2, Kind.TABLET, Kind.SAPLING))).isEqualTo(1)
        // A two-slot column not touching the bottom crafting row is still a column.
        assertThat(PanelTreeLayouts.levels(column(1, 1, Kind.TABLET, Kind.SAPLING))).isEqualTo(1)
    }

    @Test
    fun `the sapling has to be at the bottom of the column`() {
        assertThat(PanelTreeLayouts.levels(column(0, 2, Kind.SAPLING, Kind.TABLET))).isNull()
        assertThat(PanelTreeLayouts.levels(column(0, 2, Kind.TABLET, Kind.SAPLING, Kind.TABLET))).isNull()
    }

    @Test
    fun `a lone sapling seeds nothing`() {
        assertThat(PanelTreeLayouts.levels(column(0, 2, Kind.SAPLING))).isNull()
    }

    @Test
    fun `two saplings are not a tree`() {
        assertThat(PanelTreeLayouts.levels(column(0, 2, Kind.SAPLING, Kind.SAPLING))).isNull()
        assertThat(PanelTreeLayouts.levels(column(0, 2, Kind.TABLET, Kind.SAPLING, Kind.SAPLING))).isNull()
    }

    @Test
    fun `a tree panel at the bottom grows by one level per tablet`() {
        val tree1 = Slot(0, 2, Kind.TREE, levels = 1)
        assertThat(PanelTreeLayouts.levels(listOf(Slot(0, 1, Kind.TABLET), tree1))).isEqualTo(2)
        assertThat(PanelTreeLayouts.levels(listOf(Slot(0, 0, Kind.TABLET), Slot(0, 1, Kind.TABLET), tree1))).isEqualTo(3)

        val tree2 = Slot(1, 2, Kind.TREE, levels = 2)
        assertThat(PanelTreeLayouts.levels(listOf(Slot(1, 1, Kind.TABLET), tree2))).isEqualTo(3)
        // The Orchard's 16-tip tree: two crafts from a sapling.
        assertThat(PanelTreeLayouts.levels(listOf(Slot(1, 0, Kind.TABLET), Slot(1, 1, Kind.TABLET), tree2))).isEqualTo(4)
    }

    @Test
    fun `growing past the height cap rejects the craft`() {
        val tree3 = Slot(0, 2, Kind.TREE, levels = 3)
        assertThat(PanelTreeLayouts.levels(listOf(Slot(0, 0, Kind.TABLET), Slot(0, 1, Kind.TABLET), tree3))).isNull()
        val tree4 = Slot(0, 2, Kind.TREE, levels = 4)
        assertThat(PanelTreeLayouts.levels(listOf(Slot(0, 1, Kind.TABLET), tree4))).isNull()
    }

    @Test
    fun `a lone tree panel is recycle, not growth`() {
        assertThat(PanelTreeLayouts.levels(listOf(Slot(0, 2, Kind.TREE, levels = 1)))).isNull()
    }

    @Test
    fun `the tree has to be at the bottom, and a tree over a sapling is neither craft`() {
        assertThat(PanelTreeLayouts.levels(listOf(Slot(0, 1, Kind.TREE, levels = 1), Slot(0, 2, Kind.TABLET)))).isNull()
        assertThat(PanelTreeLayouts.levels(listOf(Slot(0, 1, Kind.TREE, levels = 1), Slot(0, 2, Kind.SAPLING)))).isNull()
        assertThat(PanelTreeLayouts.levels(listOf(Slot(0, 1, Kind.SAPLING), Slot(0, 2, Kind.TREE, levels = 1)))).isNull()
    }

    @Test
    fun `a gap in the column rejects the craft`() {
        val gappy: List<Slot> = listOf(Slot(0, 0, Kind.TABLET), Slot(0, 2, Kind.SAPLING))
        assertThat(PanelTreeLayouts.levels(gappy)).isNull()
    }

    @Test
    fun `slots off the column reject the craft`() {
        val bent: List<Slot> = listOf(
            Slot(0, 1, Kind.TABLET),
            Slot(0, 2, Kind.SAPLING),
            Slot(1, 2, Kind.TABLET),
        )
        assertThat(PanelTreeLayouts.levels(bent)).isNull()

        val sideways: List<Slot> = listOf(Slot(0, 2, Kind.SAPLING), Slot(1, 2, Kind.TABLET))
        assertThat(PanelTreeLayouts.levels(sideways)).isNull()
    }
}
