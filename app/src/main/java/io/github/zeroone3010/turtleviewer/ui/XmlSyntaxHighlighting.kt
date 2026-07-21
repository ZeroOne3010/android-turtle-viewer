package io.github.zeroone3010.turtleviewer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import io.github.zeroone3010.turtleviewer.lexer.XmlLexer
import io.github.zeroone3010.turtleviewer.lexer.XmlTokenType

/** Compose mapping for XML tokens, used when rendering GPX files. */
fun xmlAnnotatedString(source: String): AnnotatedString = buildAnnotatedString {
    append(source)
    XmlLexer(source).tokenize().forEach { token ->
        val color = when (token.type) {
            XmlTokenType.TAG_DELIMITER -> Color(0xFF7B1FA2)
            XmlTokenType.TAG_NAME -> Color(0xFF1565C0)
            XmlTokenType.ATTRIBUTE_NAME -> Color(0xFF00695C)
            XmlTokenType.ATTRIBUTE_VALUE, XmlTokenType.ENTITY -> Color(0xFFAD5D00)
            XmlTokenType.COMMENT, XmlTokenType.CDATA, XmlTokenType.PROCESSING_INSTRUCTION -> Color(0xFF6A737D)
            XmlTokenType.ERROR -> Color(0xFFD32F2F)
            else -> null
        }
        if (color != null && token.start < token.end) addStyle(SpanStyle(color = color), token.start, token.end)
    }
}
