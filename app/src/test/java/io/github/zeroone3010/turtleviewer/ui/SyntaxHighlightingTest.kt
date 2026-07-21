package io.github.zeroone3010.turtleviewer.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class SyntaxHighlightingTest {
    @Test fun `uses high contrast Turtle token colors in dark mode`() {
        val highlighted = turtleAnnotatedString("<https://example.test/>", darkSyntaxColors)

        assertEquals(Color(0xFF90CAF9), highlighted.spanStyles.single().item.color)
    }

    @Test fun `uses high contrast XML token colors in dark mode`() {
        val highlighted = xmlAnnotatedString("<gpx>", darkSyntaxColors)

        assertEquals(Color(0xFF90CAF9), highlighted.spanStyles.first { it.start == 1 }.item.color)
    }
}
