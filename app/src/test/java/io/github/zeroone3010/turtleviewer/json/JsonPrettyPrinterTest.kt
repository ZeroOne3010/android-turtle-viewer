package io.github.zeroone3010.turtleviewer.json

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonPrettyPrinterTest {
    @Test fun `formats nested objects and arrays`() {
        assertEquals(
            """{
  "items": [
    1,
    {
      "ok": true
    }
  ]
}""",
            JsonPrettyPrinter.format("{\"items\":[1,{\"ok\":true}]}")
        )
    }

    @Test fun `preserves punctuation and escapes inside strings`() {
        assertEquals(
            """{
  "text": "a,{b}:[c]\"d"
}""",
            JsonPrettyPrinter.format("{ \"text\" : \"a,{b}:[c]\\\"d\" }")
        )
    }

    @Test fun `keeps empty containers compact`() {
        assertEquals("{\n  \"empty\": {},\n  \"list\": []\n}", JsonPrettyPrinter.format("{\"empty\":{},\"list\":[]}"))
    }

    @Test fun `caps indentation for deeply nested input`() {
        val depth = 10_000
        val formatted = JsonPrettyPrinter.format("[".repeat(depth) + "]".repeat(depth))

        val longestIndent = formatted.lineSequence()
            .maxOf { line -> line.indexOfFirst { it != ' ' }.let { if (it < 0) line.length else it } }
        assertEquals(80, longestIndent)
        // With two output lines per level, bounded indentation keeps this comfortably linear.
        assertTrue(formatted.length < 2_000_000)
    }
}
