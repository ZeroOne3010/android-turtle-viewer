package io.github.zeroone3010.turtleviewer.lexer

import org.junit.Assert.assertEquals
import org.junit.Test

class XmlLexerTest {
    @Test fun `recognizes GPX tags attributes entities and comments`() {
        val source = "<?xml version=\"1.0\"?><gpx version=\"1.1\"><trkpt lat=\"1.2\">A &amp; B</trkpt><!-- end --></gpx>"
        assertEquals(
            listOf(
                XmlTokenType.PROCESSING_INSTRUCTION, XmlTokenType.TAG_DELIMITER, XmlTokenType.TAG_NAME,
                XmlTokenType.ATTRIBUTE_NAME, XmlTokenType.EQUALS, XmlTokenType.ATTRIBUTE_VALUE, XmlTokenType.TAG_DELIMITER,
                XmlTokenType.TAG_DELIMITER, XmlTokenType.TAG_NAME, XmlTokenType.ATTRIBUTE_NAME, XmlTokenType.EQUALS,
                XmlTokenType.ATTRIBUTE_VALUE, XmlTokenType.TAG_DELIMITER, XmlTokenType.TEXT, XmlTokenType.ENTITY,
                XmlTokenType.TEXT, XmlTokenType.TAG_DELIMITER, XmlTokenType.TAG_DELIMITER, XmlTokenType.TAG_NAME,
                XmlTokenType.TAG_DELIMITER, XmlTokenType.COMMENT, XmlTokenType.TAG_DELIMITER, XmlTokenType.TAG_DELIMITER,
                XmlTokenType.TAG_NAME, XmlTokenType.TAG_DELIMITER
            ),
            XmlLexer(source).tokenize().map { it.type }
        )
    }

    @Test fun `marks incomplete XML constructs as errors`() {
        assertEquals(XmlTokenType.ERROR, XmlLexer("<gpx version=\"1.1").tokenize().single().type)
        assertEquals(XmlTokenType.ERROR, XmlLexer("<!-- unfinished").tokenize().single().type)
    }
}
