package com.xfastgames.witness.items.renderer

import net.minecraft.util.Util
import net.minecraft.core.BlockPos

/**
 * Client-only attract pulse for tutorial panels. Solver fires it with the focused frame's pos;
 * [PuzzlePanelRenderer] draws it only on the matching frame.
 *
 * Target is stored as raw x/y/z ints (not a BlockPos / packed long) so matching cannot fail on
 * mutable pos identity or packing quirks.
 */
object PanelAttractPulse {

    enum class Kind { START, END }

    data class Sample(
        val kind: Kind,
        /** 0 at trigger, 1 when fully expanded and faded. */
        val progress: Float,
        /** Peak brightness, matching the startpoint audio volume steps. */
        val strength: Float,
    )

    private const val DURATION_MS = 700L

    private var hasTarget: Boolean = false
    private var targetX: Int = 0
    private var targetY: Int = 0
    private var targetZ: Int = 0
    private var kind: Kind? = null
    private var startedAtMs: Long = 0
    private var strength: Float = 1f

    fun triggerStart(pos: BlockPos, strength: Float = 1f) = trigger(pos, Kind.START, strength)

    fun triggerEnd(pos: BlockPos, strength: Float = 1f) = trigger(pos, Kind.END, strength)

    fun clear() {
        hasTarget = false
        kind = null
    }

    fun isFor(pos: BlockPos): Boolean =
        hasTarget && pos.x == targetX && pos.y == targetY && pos.z == targetZ

    private fun trigger(pos: BlockPos, kind: Kind, strength: Float) {
        hasTarget = true
        targetX = pos.x
        targetY = pos.y
        targetZ = pos.z
        this.kind = kind
        startedAtMs = Util.getMillis()
        this.strength = strength.coerceIn(0f, 1f)
    }

    /**
     * Progress for a live pulse aimed at [pos]. Pure read of time; does not clear mid-pass so
     * multiple render layers can sample the same frame.
     */
    fun sample(pos: BlockPos, nowMs: Long = Util.getMillis()): Sample? {
        if (!isFor(pos)) return null
        val active: Kind = kind ?: return null
        val progress: Float = (nowMs - startedAtMs).toFloat() / DURATION_MS
        if (progress >= 1f) {
            clear()
            return null
        }
        return Sample(active, progress.coerceIn(0f, 1f), strength)
    }
}
