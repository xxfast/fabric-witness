package com.xfastgames.witness.screens.widgets

import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.screens.widgets.WRadioImageButton.RenderState
import org.junit.jupiter.api.Test

class WRadioImageButtonTests {

    @Test
    fun `a disabled tool draws dark even while it is the armed one`() {
        assertThat(WRadioImageButton.renderStateFor(enabled = false, selected = true, hovered = false))
            .isEqualTo(RenderState.Disabled)
    }

    @Test
    fun `a disabled tool does not light up under the cursor`() {
        assertThat(WRadioImageButton.renderStateFor(enabled = false, selected = false, hovered = true))
            .isEqualTo(RenderState.Disabled)
    }

    @Test
    fun `an enabled tool is selected over highlighted over normal`() {
        assertThat(WRadioImageButton.renderStateFor(enabled = true, selected = true, hovered = true))
            .isEqualTo(RenderState.Selected)
        assertThat(WRadioImageButton.renderStateFor(enabled = true, selected = false, hovered = true))
            .isEqualTo(RenderState.Highlighted)
        assertThat(WRadioImageButton.renderStateFor(enabled = true, selected = false, hovered = false))
            .isEqualTo(RenderState.Normal)
    }
}
