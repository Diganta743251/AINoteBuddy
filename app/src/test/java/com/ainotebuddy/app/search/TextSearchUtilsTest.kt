package com.ainotebuddy.app.search

import org.junit.Assert.assertEquals
import org.junit.Test

class TextSearchUtilsTest {

    @Test
    fun `WS_REGEX matches whitespace consistently`() {
        val expected = listOf("foo", "bar", "baz")
        val actual = "foo   bar\tbaz\n".split(WS_REGEX)
        assertEquals(expected, actual)
    }
}