package com.xfastgames.witness.items.renderer

import net.minecraft.util.Util
import net.minecraft.core.BlockPos

/**
 * Client-only red flash for failed symbols. Solver fires it with the frame that failed;
 * [PuzzlePanelRenderer] draws it only on that frame.
 *
 * Target is raw x/y/z ints so matching cannot fail on BlockPos identity.
 */
object PanelErrorFlash {

    /** The outline a failed symbol blinks in: the symbol's own shape, so a square reads as a square. */
    enum class Shape { HEXAGON, SQUARE }

    /** One failed symbol: where it sits in panel units, and what to draw there. */
    data class Mark(val x: Float, val y: Float, val shape: Shape)

    data class Sample(
        val marks: List<Mark>,
        /** 0..1; 0 means this blink trough is off. */
        val alpha: Float,
    )

    private const val DURATION_MS = 1_600L
    private const val BLINKS = 4

    private var hasTarget: Boolean = false
    private var targetX: Int = 0
    private var targetY: Int = 0
    private var targetZ: Int = 0
    private var marks: List<Mark> = emptyList()
    private var startedAtMs: Long = 0

    fun trigger(pos: BlockPos, failed: List<Mark>) {
        if (failed.isEmpty()) return
        hasTarget = true
        targetX = pos.x
        targetY = pos.y
        targetZ = pos.z
        marks = failed
        startedAtMs = Util.getMillis()
    }

    fun clear() {
        hasTarget = false
        marks = emptyList()
    }

    fun isFor(pos: BlockPos): Boolean =
        hasTarget && pos.x == targetX && pos.y == targetY && pos.z == targetZ

    fun sample(pos: BlockPos, nowMs: Long = Util.getMillis()): Sample? {
        if (!isFor(pos) || marks.isEmpty()) return null
        val progress: Float = (nowMs - startedAtMs).toFloat() / DURATION_MS
        if (progress >= 1f) {
            clear()
            return null
        }
        // Square blink: long "on" window so a short glance still catches red.
        val cycle: Float = (progress * BLINKS) % 1f
        val alpha: Float = if (cycle < 0.7f) 1f else 0f
        return Sample(marks, alpha)
    }
}
