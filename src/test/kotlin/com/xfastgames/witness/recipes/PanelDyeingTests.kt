package com.xfastgames.witness.recipes

import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.recipes.PanelDyeing.Target
import org.junit.jupiter.api.Test

/** rules/minecraft/02-panel-dye.md#the-rule and #edge-cases. */
class PanelDyeingTests {

    @Test
    fun `a panel and a dye recolour the background`() {
        assertThat(PanelDyeing.target(panels = 1, dyes = 1, glowInkSacs = 0, others = 0)).isEqualTo(Target.BACKGROUND)
    }

    @Test
    fun `a glow ink sac with the dye recolours the line instead`() {
        assertThat(PanelDyeing.target(panels = 1, dyes = 1, glowInkSacs = 1, others = 0)).isEqualTo(Target.LINE)
    }

    @Test
    fun `a glow ink sac without a dye is no craft`() {
        assertThat(PanelDyeing.target(panels = 1, dyes = 0, glowInkSacs = 1, others = 0)).isNull()
    }

    @Test
    fun `the counts have to be exact`() {
        assertThat(PanelDyeing.target(panels = 2, dyes = 1, glowInkSacs = 0, others = 0)).isNull()
        assertThat(PanelDyeing.target(panels = 1, dyes = 2, glowInkSacs = 0, others = 0)).isNull()
        assertThat(PanelDyeing.target(panels = 1, dyes = 1, glowInkSacs = 2, others = 0)).isNull()
        assertThat(PanelDyeing.target(panels = 0, dyes = 1, glowInkSacs = 1, others = 0)).isNull()
    }

    @Test
    fun `a stray item fails either craft`() {
        assertThat(PanelDyeing.target(panels = 1, dyes = 1, glowInkSacs = 0, others = 1)).isNull()
        assertThat(PanelDyeing.target(panels = 1, dyes = 1, glowInkSacs = 1, others = 1)).isNull()
    }
}
