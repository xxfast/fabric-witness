package com.xfastgames.witness.utils

import org.junit.jupiter.api.Test

class ClosedFloatingPointRangeTests {

    @Test
    fun testFloatRangeFullyInRange() {
        val testRange = 0.5f..1.0f
        val intersectRange = 0.65f..0.85f
        assert(intersectRange intersects testRange)
    }

    @Test
    fun testFloatRangeMinOverlap() {
        val testRange = 0.5f..1.0f
        val intersectRange = 0.45f..0.85f
        assert(intersectRange intersects testRange)
    }

    @Test
    fun testFloatRangeMaxOverlap() {
        val testRange = 0.5f..1.0f
        val intersectRange = 0.65f..1.25f
        assert(intersectRange intersects testRange)
    }

    @Test
    fun testFloatRangeNoOverlap() {
        val testRange = 0.5f..1.0f
        val intersectRange = 1.25f..1.55f
        assert(!(intersectRange intersects testRange))
    }
}