package com.xfastgames.witness.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CollectionTests {

    @Test
    fun testClosestInt() {
        val values: List<Int> = listOf(1, 2, 3, 4, 5, 7, 8, 9, 10)
        val actual: Int? = values.closest(6)
        val expect = 7
        assertThat(actual).isEqualTo(expect)
    }

    @Test
    fun testClosestDouble() {
        val values: List<Double> = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
        val actual: Double? = values.closest(5.5)
        val expect = 6.0
        assertThat(actual).isEqualTo(expect)
    }
}