package com.lwl.mediacodectest

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
        val widht = 1922
        val height = 1080
        val w = (widht / 2) * 2
        val h = (height / 2) * 2
        println("width $w  height $h")
    }
}