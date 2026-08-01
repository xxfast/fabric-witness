package com.xfastgames.witness.items.renderer

import net.minecraft.util.Util

/**
 * Client-only attract pulse for tutorial panels. The solver screen fires it on the same beat as
 * `panel_scint_startpoint` / `panel_scint_endpoint`; [PuzzlePanelRenderer] samples it each frame
 * and draws the expanding white ring.
 *
 * Only one pulse is live at a time (focus mode has one panel), so this is a single global slot
 * rather than a map keyed by block position.
 */
object PanelAttractPulse {

    enum class Kind { START, END }

    data class Frame(
        val kind: Kind,
        /** 0 at trigger, 1 when fully expanded and faded. */
        val progress: Float,
        /** Peak brightness, matching the startpoint audio volume steps. */
        val strength: Float,
    )

    /** How long one expand-and-fade ring takes. */
    private const val DURATION_MS = 700L

    private var kind: Kind? = null
    private var startedAtMs: Long = 0
    private var strength: Float = 1f

    fun triggerStart(strength: Float = 1f) = trigger(Kind.START, strength)

    fun triggerEnd(strength: Float = 1f) = trigger(Kind.END, strength)

    fun clear() {
        kind = null
    }

    private fun trigger(kind: Kind, strength: Float) {
        this.kind = kind
        this.startedAtMs = Util.getMeasuringTimeMs()
        this.strength = strength.coerceIn(0f, 1f)
    }

    fun sample(nowMs: Long = Util.getMeasuringTimeMs()): Frame? {
        val active: Kind = kind ?: return null
        val progress: Float = (nowMs - startedAtMs).toFloat() / DURATION_MS
        if (progress >= 1f) {
            kind = null
            return null
        }
        return Frame(active, progress.coerceIn(0f, 1f), strength)
    }
}
