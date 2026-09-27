package com.xfastgames.witness.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/** The screen frame's tube: 0.91 of the glass, bowed 4% of its half side (rules/minecraft/05-1-screen-frame.md). */
class CurveTests {

    private val half: Float = 0.455f
    private val curve = Curve(half = half, bulge = 0.04f)

    @Test
    fun theCentreStaysPut() {
        val (x, y) = curve.bend(0.5f, 0.5f)
        assertThat(x).isWithin(1e-6f).of(0.5f)
        assertThat(y).isWithin(1e-6f).of(0.5f)
    }

    @Test
    fun theMiddleOfAnEdgeMovesOutTwoPercentOfTheSide() {
        val (x, y) = curve.bend(0.5f + half, 0.5f)
        assertThat(x - (0.5f + half)).isWithin(1e-6f).of(0.02f * 2 * half)
        assertThat(y).isWithin(1e-6f).of(0.5f)
    }

    @Test
    fun theCornersStayPut() {
        val (x, y) = curve.bend(0.5f - half, 0.5f + half)
        assertThat(x).isWithin(1e-6f).of(0.5f - half)
        assertThat(y).isWithin(1e-6f).of(0.5f + half)
    }

    @Test
    fun theGlassPastTheCornersIsFlat() {
        val (x, y) = curve.bend(0f, 1f)
        assertThat(x).isWithin(1e-6f).of(0f)
        assertThat(y).isWithin(1e-6f).of(1f)
    }

    @Test
    fun anEdgeBowsOutwardsBetweenItsCorners() {
        val (atCorner, _) = curve.bend(0.5f + half, 0.5f + half)
        val (atQuarter, _) = curve.bend(0.5f + half, 0.5f + half / 2)
        val (atMiddle, _) = curve.bend(0.5f + half, 0.5f)
        assertThat(atQuarter).isGreaterThan(atCorner)
        assertThat(atMiddle).isGreaterThan(atQuarter)
    }

    @Test
    fun aScaledPassBendsTheSamePointOnTheFace() {
        // A lattice on a 4-wide panel is drawn at a quarter scale: pass coordinate 2 is the face's centre.
        val scaled: Curve = curve.scaled(0.25f)
        val (x, _) = scaled.bend((0.5f + half) / 0.25f, 2f)
        val (flat, _) = curve.bend(0.5f + half, 0.5f)
        assertThat(x * 0.25f).isWithin(1e-5f).of(flat)
    }
}
