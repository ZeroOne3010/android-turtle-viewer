package io.github.zeroone3010.turtleviewer.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import io.github.zeroone3010.turtleviewer.lexer.TurtleScanner
import io.github.zeroone3010.turtleviewer.lexer.TurtleTokenType

/** Compose-only mapping layer; the lexer remains usable in plain JVM code. */
fun turtleAnnotatedString(source: String, colors: SyntaxColors = lightSyntaxColors): AnnotatedString = buildAnnotatedString {
    append(source)
    TurtleScanner(source).forEach { token ->
        val color = when (token.type) {
            TurtleTokenType.COMMENT -> colors.comment
            TurtleTokenType.PREFIX_DIRECTIVE, TurtleTokenType.BASE_DIRECTIVE,
            TurtleTokenType.SPARQL_PREFIX, TurtleTokenType.SPARQL_BASE, TurtleTokenType.KEYWORD_A -> colors.keyword
            TurtleTokenType.IRI -> colors.iri
            TurtleTokenType.PREFIX_NAME, TurtleTokenType.BLANK_NODE_LABEL -> colors.name
            TurtleTokenType.STRING, TurtleTokenType.LONG_STRING, TurtleTokenType.LANGUAGE_TAG -> colors.string
            TurtleTokenType.INTEGER, TurtleTokenType.DECIMAL, TurtleTokenType.DOUBLE, TurtleTokenType.BOOLEAN -> colors.number
            TurtleTokenType.ERROR -> colors.error
            else -> null
        }
        if (color != null && token.start < token.end) addStyle(SpanStyle(color = color), token.start, token.end)
    }
}
