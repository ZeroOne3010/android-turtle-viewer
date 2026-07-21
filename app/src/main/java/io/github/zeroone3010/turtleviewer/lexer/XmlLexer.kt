package io.github.zeroone3010.turtleviewer.lexer

/** A lightweight XML lexer for highlighting XML-based file formats such as GPX. */
class XmlLexer(private val source: CharSequence) {
    fun tokenize(): List<XmlToken> {
        val tokens = mutableListOf<XmlToken>()
        var index = 0
        fun emit(type: XmlTokenType, start: Int, end: Int) { tokens += XmlToken(type, start, end) }
        while (index < source.length) {
            val start = index
            when {
                source.startsWith("<!--", index) -> {
                    val end = source.indexOf("-->", index + 4)
                    index = if (end >= 0) end + 3 else source.length
                    emit(if (end >= 0) XmlTokenType.COMMENT else XmlTokenType.ERROR, start, index)
                }
                source.startsWith("<![CDATA[", index) -> {
                    val end = source.indexOf("]]>", index + 9)
                    index = if (end >= 0) end + 3 else source.length
                    emit(if (end >= 0) XmlTokenType.CDATA else XmlTokenType.ERROR, start, index)
                }
                source.startsWith("<?", index) -> {
                    val end = source.indexOf("?>", index + 2)
                    index = if (end >= 0) end + 2 else source.length
                    emit(if (end >= 0) XmlTokenType.PROCESSING_INSTRUCTION else XmlTokenType.ERROR, start, index)
                }
                source[index] == '<' -> {
                    val end = findTagEnd(index)
                    if (end < 0) { index = source.length; emit(XmlTokenType.ERROR, start, index) }
                    else { scanTag(index, end + 1, ::emit); index = end + 1 }
                }
                source[index] == '&' -> {
                    index = (source.indexOf(';', index + 1).takeIf { it >= 0 } ?: index) + 1
                    emit(if (source[index - 1] == ';') XmlTokenType.ENTITY else XmlTokenType.ERROR, start, index)
                }
                else -> { index++; while (index < source.length && source[index] != '<' && source[index] != '&') index++; emit(XmlTokenType.TEXT, start, index) }
            }
        }
        return tokens
    }

    /** Finds a tag terminator while leaving greater-than characters inside quoted values alone. */
    private fun findTagEnd(start: Int): Int {
        var index = start + 1
        var quote: Char? = null
        while (index < source.length) {
            val character = source[index]
            when {
                quote != null && character == quote -> quote = null
                quote == null && (character == '\'' || character == '"') -> quote = character
                quote == null && character == '>' -> return index
            }
            index++
        }
        return -1
    }

    private fun scanTag(start: Int, end: Int, emit: (XmlTokenType, Int, Int) -> Unit) {
        var index = start
        emit(XmlTokenType.TAG_DELIMITER, index, ++index)
        if (index < end && source[index] == '/') emit(XmlTokenType.TAG_DELIMITER, index, ++index)
        while (index < end - 1 && source[index].isWhitespace()) index++
        val nameStart = index
        while (index < end - 1 && isNameChar(source[index])) index++
        if (nameStart == index) { emit(XmlTokenType.ERROR, nameStart, end); return }
        emit(XmlTokenType.TAG_NAME, nameStart, index)
        while (index < end - 1) {
            while (index < end - 1 && source[index].isWhitespace()) index++
            if (index >= end - 1 || source[index] == '/') break
            val attributeStart = index
            while (index < end - 1 && isNameChar(source[index])) index++
            if (attributeStart == index) { emit(XmlTokenType.ERROR, index, end - 1); break }
            emit(XmlTokenType.ATTRIBUTE_NAME, attributeStart, index)
            while (index < end - 1 && source[index].isWhitespace()) index++
            if (index < end - 1 && source[index] == '=') {
                emit(XmlTokenType.EQUALS, index, ++index)
                while (index < end - 1 && source[index].isWhitespace()) index++
                val valueStart = index
                if (index < end - 1 && (source[index] == '\'' || source[index] == '"')) {
                    val quote = source[index++]
                    while (index < end - 1 && source[index] != quote) index++
                    if (index < end - 1) index++
                }
                emit(if (index > valueStart && source[index - 1] in "\'\"") XmlTokenType.ATTRIBUTE_VALUE else XmlTokenType.ERROR, valueStart, index)
            }
        }
        if (end - start >= 2 && source[end - 2] == '/') emit(XmlTokenType.TAG_DELIMITER, end - 2, end - 1)
        emit(XmlTokenType.TAG_DELIMITER, end - 1, end)
    }

    private fun isNameChar(char: Char) = char.isLetterOrDigit() || char in "_:-."
}

data class XmlToken(val type: XmlTokenType, val start: Int, val end: Int)
enum class XmlTokenType { TAG_DELIMITER, TAG_NAME, ATTRIBUTE_NAME, EQUALS, ATTRIBUTE_VALUE, COMMENT, CDATA, PROCESSING_INSTRUCTION, ENTITY, TEXT, ERROR }
