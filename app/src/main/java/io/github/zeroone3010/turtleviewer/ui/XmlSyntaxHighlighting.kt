package io.github.zeroone3010.turtleviewer.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import io.github.zeroone3010.turtleviewer.lexer.XmlLexer
import io.github.zeroone3010.turtleviewer.lexer.XmlTokenType

/** Compose mapping for XML tokens, used when rendering GPX files. */
fun xmlAnnotatedString(source: String, colors: SyntaxColors = lightSyntaxColors): AnnotatedString = buildAnnotatedString {
    append(source)
    XmlLexer(source).tokenize().forEach { token ->
        val color = when (token.type) {
            XmlTokenType.TAG_DELIMITER -> colors.keyword
            XmlTokenType.TAG_NAME -> colors.iri
            XmlTokenType.ATTRIBUTE_NAME -> colors.name
            XmlTokenType.ATTRIBUTE_VALUE, XmlTokenType.ENTITY -> colors.string
            XmlTokenType.COMMENT, XmlTokenType.CDATA, XmlTokenType.PROCESSING_INSTRUCTION -> colors.comment
            XmlTokenType.ERROR -> colors.error
            else -> null
        }
        if (color != null && token.start < token.end) addStyle(SpanStyle(color = color), token.start, token.end)
    }
}
