package io.github.draumaz.nickel.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryShapesTest {

    /** Spinning badges must all sweep the same circle, or some look bigger than others. */
    @Test
    fun everyShapeSweepsTheSameCircle() {
        for (key in CategoryShapes.keys + "cookie4" + "cookie9" + "pill" + "arch" + "burst" + "ghostish") {
            val (left, top, right, bottom) = CategoryShapes.get(key).calculateMaxBounds()
            assertEquals("$key diameter", 1f, right - left, 0.01f)
            assertEquals("$key centre x", 0.5f, (left + right) / 2, 0.01f)
            assertEquals("$key centre y", 0.5f, (top + bottom) / 2, 0.01f)
        }
    }
}
