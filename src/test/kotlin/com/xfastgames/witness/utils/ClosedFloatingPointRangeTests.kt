package com.xfastgames.witness.utils

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ClosedFloatingPointRangeTests {

    @Nested
    inner class TestIntersect {
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

    @Nested
    inner class TestIntersection {

        @Test
        fun testFloatRangePlusIntersection() {
            val testRange = 0.5f..1.0f
            val intersectRange = 0.75f..1.5f
            val intersection = testRange intersection intersectRange
            assert(intersection == 0.75f..1.0f)
        }

        @Test
        fun testFloatRangeMinusIntersection() {
            val testRange = 0.5f..1.0f
            val intersectRange = 0.25f..0.75f
            val intersection = testRange intersection intersectRange
            assert(intersection == 0.5f..0.75f)
        }

        @Test
        fun testFloatRangeFullIntersection() {
            val testRange = 0.5f..1.0f
            val intersectRange = 0.55f..0.75f
            val intersection = testRange intersection intersectRange
            assert(intersection == intersectRange)
        }

        @Test
        fun testFloatRangeNoIntersection() {
            val testRange = 0.5f..1.0f
            val intersectRange = 0.25f..0.45f
            val intersection = testRange intersection intersectRange
            assert(intersection == null)
        }
    }
}