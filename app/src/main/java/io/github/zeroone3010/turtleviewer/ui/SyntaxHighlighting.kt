package io.github.zeroone3010.turtleviewer.ui

import androidx.compose.ui.text.AnnotatedString

/** Selects syntax highlighting by the recognized file format. */
enum class SyntaxFormat { TURTLE, XML }

fun annotatedString(source: String, format: SyntaxFormat): AnnotatedString = when (format) {
    SyntaxFormat.TURTLE -> turtleAnnotatedString(source)
    SyntaxFormat.XML -> xmlAnnotatedString(source)
}
