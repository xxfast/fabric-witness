package com.xfastgames.witness.utils

/**
 * The bow of a television tube (rules/minecraft/05-1-screen-frame.md#the-picture): everything on
 * the face is pushed out from the centre by a factor that is largest in the middle and falls to
 * none at the tube's corners, so the corners stay put and every edge bows outwards between them.
 *
 * Coordinates are the face's own, 0..1 across the glass with the tube centred on 0.5, multiplied
 * by [unit] first: a pass drawn at 1/maxDimension scale bends through a curve with that [unit].
 *
 * @param half the tube's half side on the face.
 * @param bulge how far the middle of an edge moves out, as a fraction of [half].
 */
data class Curve(val half: Float, val bulge: Float, val unit: Float = 1f) {

    fun scaled(unit: Float): Curve = copy(unit = unit)

    /** Where the point at ([x], [y]) in pass coordinates is drawn once bent. */
    fun bend(x: Float, y: Float): Pair<Float, Float> {
        val factor: Float = factorAt(x, y)
        return along(x, factor) to along(y, factor)
    }

    /**
     * How far the point at ([x], [y]) is pushed out from the centre, for [along]. Split from [bend]
     * so a hot loop can bend a vertex without allocating a pair for it.
     */
    fun factorAt(x: Float, y: Float): Float {
        val dx: Float = (x * unit - CENTRE) / half
        val dy: Float = (y * unit - CENTRE) / half
        // Past the tube's corner there is only glass, which is flat, so the factor stops at 1.
        val radiusSquared: Float = (dx * dx + dy * dy).coerceAtMost(CORNER_RADIUS_SQUARED)
        return 1f + bulge * (CORNER_RADIUS_SQUARED - radiusSquared)
    }

    /** One coordinate of a point pushed out by [factor]. */
    fun along(coordinate: Float, factor: Float): Float = (CENTRE + (coordinate * unit - CENTRE) * factor) / unit

    companion object {
        private const val CENTRE: Float = 0.5f

        /** A tube corner, in [half]s from the centre: one along each axis. */
        private const val CORNER_RADIUS_SQUARED: Float = 2f
    }
}
