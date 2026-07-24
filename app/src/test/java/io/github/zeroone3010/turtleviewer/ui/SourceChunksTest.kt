package io.github.zeroone3010.turtleviewer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceChunksTest {
    @Test fun `retains all source text in bounded chunks`() {
        val source = "first\n" + "x".repeat(5_000) + "\nlast"
        val chunks = SourceChunks.from(source, maxChunkLength = 1_000)

        assertEquals(source, chunks.ranges.joinToString("") { source.substring(it.first, it.last + 1) })
        assertTrue(chunks.ranges.all { it.last - it.first + 1 <= 1_000 })
    }

    @Test fun `prefers a nearby line boundary`() {
        val source = "a".repeat(600) + "\n" + "b".repeat(600)
        val chunks = SourceChunks.from(source, maxChunkLength = 1_000)

        assertEquals(601, chunks.ranges.first().last + 1)
    }
}
