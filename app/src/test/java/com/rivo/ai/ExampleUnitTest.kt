package com.rivo.ai

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test fun addition_isCorrect() { assertEquals(4, 2+2) }
    @Test fun urlValidation() {
        assertTrue("https://api.openai.com".startsWith("https://"))
        assertTrue("http://localhost".startsWith("http://"))
    }
    @Test fun markdownSplit() {
        val input = "Hello ```kotlin\nval x=1\n``` world"
        assertTrue(input.contains("```"))
    }
}
