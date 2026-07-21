package io.github.zeroone3010.turtleviewer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class VisibleWhitespaceTest {
    @Test fun `makes whitespace visible while preserving syntax highlight spans`() {
        val highlighted = AnnotatedString.Builder("a b\tc").apply {
            addStyle(SpanStyle(color = Color.Red), 2, 5)
        }.toAnnotatedString()

        val visible = highlighted.withVisibleWhitespace()

        assertEquals("a·b→\tc", visible.text)
        assertEquals(1, visible.spanStyles.size)
        assertEquals(Color.Red, visible.spanStyles.single().item.color)
        assertEquals(2, visible.spanStyles.single().start)
        assertEquals(6, visible.spanStyles.single().end)
    }
}
