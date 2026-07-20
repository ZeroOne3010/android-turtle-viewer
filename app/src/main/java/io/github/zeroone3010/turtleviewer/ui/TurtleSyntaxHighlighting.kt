package io.github.zeroone3010.turtleviewer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import io.github.zeroone3010.turtleviewer.lexer.TurtleLexer
import io.github.zeroone3010.turtleviewer.lexer.TurtleTokenType

/** Compose-only mapping layer; the lexer remains usable in plain JVM code. */
fun turtleAnnotatedString(source: String): AnnotatedString = buildAnnotatedString {
    append(source)
    TurtleLexer(source).tokenize().forEach { token ->
        val color = when (token.type) {
            TurtleTokenType.COMMENT -> Color(0xFF6A737D)
            TurtleTokenType.PREFIX_DIRECTIVE, TurtleTokenType.BASE_DIRECTIVE,
            TurtleTokenType.SPARQL_PREFIX, TurtleTokenType.SPARQL_BASE, TurtleTokenType.KEYWORD_A -> Color(0xFF7B1FA2)
            TurtleTokenType.IRI -> Color(0xFF1565C0)
            TurtleTokenType.PREFIX_NAME, TurtleTokenType.BLANK_NODE_LABEL -> Color(0xFF00695C)
            TurtleTokenType.STRING, TurtleTokenType.LONG_STRING, TurtleTokenType.LANGUAGE_TAG -> Color(0xFFAD5D00)
            TurtleTokenType.INTEGER, TurtleTokenType.DECIMAL, TurtleTokenType.DOUBLE, TurtleTokenType.BOOLEAN -> Color(0xFFB00020)
            TurtleTokenType.ERROR -> Color(0xFFD32F2F)
            else -> null
        }
        if (color != null && token.start < token.end) addStyle(SpanStyle(color = color), token.start, token.end)
    }
}
