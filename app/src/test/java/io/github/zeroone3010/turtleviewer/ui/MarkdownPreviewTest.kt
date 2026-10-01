package io.github.zeroone3010.turtleviewer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownPreviewTest {
    @Test fun `short Markdown remains unchanged`() {
        assertEquals("# Heading", markdownPreview("# Heading"))
    }

    @Test fun `large Markdown preview is bounded and explains truncation`() {
        val markdown = "a".repeat(MarkdownPreviewCharacterLimit + 500)

        val preview = markdownPreview(markdown)

        assertTrue(preview.startsWith("a".repeat(MarkdownPreviewCharacterLimit)))
        assertTrue(preview.contains("Rendered preview truncated"))
        assertTrue(preview.contains("complete document remains available in Source"))
        assertTrue(preview.length < markdown.length)
    }
}
