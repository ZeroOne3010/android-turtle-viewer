package io.github.zeroone3010.turtleviewer.lexer

/** A half-open range in the source text. Text is deliberately not copied. */
data class TurtleToken(
    val type: TurtleTokenType,
    val start: Int,
    val end: Int,
    val line: Int,
    val column: Int
)
