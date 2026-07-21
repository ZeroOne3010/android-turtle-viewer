package io.github.zeroone3010.turtleviewer.lexer

import org.junit.Assert.assertEquals
import org.junit.Test

class TurtleLexerTest {
    private fun tokens(text: String) = TurtleLexer(text).tokenize().filter { it.type != TurtleTokenType.WHITESPACE && it.type != TurtleTokenType.EOF }
    private fun types(text: String) = tokens(text).map { it.type }

    @Test fun recognizesCommonTurtleForms() {
        assertEquals(listOf(
            TurtleTokenType.PREFIX_DIRECTIVE, TurtleTokenType.PREFIX_NAME, TurtleTokenType.IRI,
            TurtleTokenType.PREFIX_NAME, TurtleTokenType.KEYWORD_A, TurtleTokenType.PREFIX_NAME,
            TurtleTokenType.SEMICOLON, TurtleTokenType.PREFIX_NAME, TurtleTokenType.STRING,
            TurtleTokenType.LANGUAGE_TAG, TurtleTokenType.DOT, TurtleTokenType.COMMENT
        ), types("@prefix ex: <https://example.test/>\nex:s a ex:Thing; ex:name \"hello\"@en. # done"))
    }

    @Test fun recognizesLiteralNumericAndPunctuationForms() {
        assertEquals(listOf(
            TurtleTokenType.LONG_STRING, TurtleTokenType.LONG_STRING, TurtleTokenType.BLANK_NODE_LABEL,
            TurtleTokenType.INTEGER, TurtleTokenType.DECIMAL, TurtleTokenType.DOUBLE, TurtleTokenType.DOUBLE,
            TurtleTokenType.BOOLEAN, TurtleTokenType.BOOLEAN, TurtleTokenType.OPEN_BRACKET,
            TurtleTokenType.CLOSE_BRACKET, TurtleTokenType.OPEN_PAREN, TurtleTokenType.CLOSE_PAREN,
            TurtleTokenType.OPEN_BRACE, TurtleTokenType.CLOSE_BRACE, TurtleTokenType.COMMA
        ), types("\"\"\"one\n two\"\"\" '''three''' _:b0 12 3.14 1e5 2.3E-7 true false [](){} ,"))
    }

    @Test fun recognizesDatatypeAndSparqlDirectives() {
        assertEquals(listOf(TurtleTokenType.SPARQL_PREFIX, TurtleTokenType.PREFIX_NAME, TurtleTokenType.IRI,
            TurtleTokenType.SPARQL_BASE, TurtleTokenType.IRI, TurtleTokenType.STRING,
            TurtleTokenType.DOUBLE_CARET, TurtleTokenType.PREFIX_NAME),
            types("PREFIX xsd: <urn:x> BASE <urn:base> \"12\"^^xsd:int"))
    }

    @Test fun recognizesBaseDirectiveAndEscapedIri() {
        assertEquals(listOf(TurtleTokenType.BASE_DIRECTIVE, TurtleTokenType.IRI),
            types("@base <https://example.test/\\u0061>"))
    }

    @Test fun recognizesColonsInPrefixedNameLocalPart() {
        assertEquals(listOf(TurtleTokenType.PREFIX_NAME), types("ex:part:detail"))
    }

    @Test fun recognizesDotsInsidePrefixedNameLocalPart() {
        assertEquals(listOf(TurtleTokenType.PREFIX_NAME, TurtleTokenType.DOT),
            types("ex:release.1."))
    }

    @Test fun tracksSourcePositions() {
        val result = TurtleLexer("# c\n  ex:name").tokenize()
        val prefix = result.first { it.type == TurtleTokenType.PREFIX_NAME }
        assertEquals(6, prefix.start); assertEquals(13, prefix.end)
        assertEquals(2, prefix.line); assertEquals(3, prefix.column)
    }

    @Test fun reportsMalformedInputAndRecovers() {
        val result = TurtleLexer("\"unterminated\n<bad\n12oops @sv- $ ex:ok").tokenize()
        assertEquals(listOf(TurtleTokenType.ERROR, TurtleTokenType.WHITESPACE, TurtleTokenType.ERROR,
            TurtleTokenType.WHITESPACE, TurtleTokenType.ERROR, TurtleTokenType.WHITESPACE,
            TurtleTokenType.ERROR, TurtleTokenType.WHITESPACE, TurtleTokenType.ERROR,
            TurtleTokenType.WHITESPACE, TurtleTokenType.PREFIX_NAME, TurtleTokenType.EOF), result.map { it.type })
    }

    @Test fun reportsInvalidQuotedLiteralEscapes() {
        assertEquals(listOf(TurtleTokenType.ERROR, TurtleTokenType.ERROR, TurtleTokenType.ERROR),
            types("\"bad\\q\" \"bad\\u12G4\" \"\"\"bad\\U123\"\"\""))
    }

    @Test fun reportsWhitespaceInIriReferences() {
        assertEquals(listOf(TurtleTokenType.ERROR, TurtleTokenType.ERROR),
            types("<https://example.test/a b> <a\tb>"))
    }

    @Test fun randomMalformedTextAlwaysReachesEof() {
        val result = TurtleLexer("@@@ ^^ _:. <unterminated \\").tokenize()
        assertEquals(TurtleTokenType.EOF, result.last().type)
        assertEquals(result.size - 1, result.indexOfLast { it.type == TurtleTokenType.EOF })
    }
}
