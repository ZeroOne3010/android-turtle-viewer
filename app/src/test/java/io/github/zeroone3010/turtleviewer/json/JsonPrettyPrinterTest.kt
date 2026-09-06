package io.github.zeroone3010.turtleviewer.json

import org.junit.Assert.assertEquals
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
}
