package com.xfastgames.witness.items.renderer

import com.xfastgames.witness.items.data.Hexagon
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos

/**
 * Client-only red flash for missed hexagon dots. Solver fires it with the frame that failed;
 * [PuzzlePanelRenderer] draws it only on that frame.
 *
 * Target is raw x/y/z ints so matching cannot fail on BlockPos identity.
 */
object PanelErrorFlash {

    data class Frame(
        val positions: List<Pair<Float, Float>>,
        /** 0..1; 0 means this blink trough is off. */
        val alpha: Float,
    )

    private const val DURATION_MS = 1_600L
    private const val BLINKS = 4

    private var hasTarget: Boolean = false
    private var targetX: Int = 0
    private var targetY: Int = 0
    private var targetZ: Int = 0
    private var positions: List<Pair<Float, Float>> = emptyList()
    private var startedAtMs: Long = 0

    fun trigger(pos: BlockPos, missed: List<Hexagon>) {
        if (missed.isEmpty()) return
        hasTarget = true
        targetX = pos.x
        targetY = pos.y
        targetZ = pos.z
        positions = missed.map { hexagon ->
            when (hexagon) {
                is Hexagon.OnNode -> hexagon.node.x to hexagon.node.y
                is Hexagon.OnEdge ->
                    (hexagon.u.x + hexagon.v.x) / 2f to (hexagon.u.y + hexagon.v.y) / 2f
            }
        }
        startedAtMs = Util.getMeasuringTimeMs()
    }

    fun clear() {
        hasTarget = false
        positions = emptyList()
    }

    fun isFor(pos: BlockPos): Boolean =
        hasTarget && pos.x == targetX && pos.y == targetY && pos.z == targetZ

    fun sample(pos: BlockPos, nowMs: Long = Util.getMeasuringTimeMs()): Frame? {
        if (!isFor(pos) || positions.isEmpty()) return null
        val progress: Float = (nowMs - startedAtMs).toFloat() / DURATION_MS
        if (progress >= 1f) {
            clear()
            return null
        }
        // Square blink: long "on" window so a short glance still catches red.
        val cycle: Float = (progress * BLINKS) % 1f
        val alpha: Float = if (cycle < 0.7f) 1f else 0f
        return Frame(positions, alpha)
    }
}
