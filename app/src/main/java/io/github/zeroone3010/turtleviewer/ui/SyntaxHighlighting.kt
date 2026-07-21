package io.github.zeroone3010.turtleviewer.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.Color

/** Selects syntax highlighting by the recognized file format. */
enum class SyntaxFormat { TURTLE, XML }

data class SyntaxColors(
    val comment: Color,
    val keyword: Color,
    val iri: Color,
    val name: Color,
    val string: Color,
    val number: Color,
    val error: Color
)

val lightSyntaxColors = SyntaxColors(
    comment = Color(0xFF6A737D), keyword = Color(0xFF7B1FA2), iri = Color(0xFF1565C0),
    name = Color(0xFF00695C), string = Color(0xFFAD5D00), number = Color(0xFFB00020), error = Color(0xFFD32F2F)
)

val darkSyntaxColors = SyntaxColors(
    comment = Color(0xFFB0BEC5), keyword = Color(0xFFCE93D8), iri = Color(0xFF90CAF9),
    name = Color(0xFF80CBC4), string = Color(0xFFFFCC80), number = Color(0xFFEF9A9A), error = Color(0xFFFF8A80)
)

fun annotatedString(source: String, format: SyntaxFormat, colors: SyntaxColors = lightSyntaxColors): AnnotatedString = when (format) {
    SyntaxFormat.TURTLE -> turtleAnnotatedString(source, colors)
    SyntaxFormat.XML -> xmlAnnotatedString(source, colors)
}
