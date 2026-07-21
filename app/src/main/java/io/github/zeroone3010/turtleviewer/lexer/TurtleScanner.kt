package io.github.zeroone3010.turtleviewer.lexer

/**
 * A non-recursive, cursor-based Turtle lexer. It recognizes lexical forms only:
 * RDF term validity and triple grammar are intentionally left to RDF4J.
 */
class TurtleScanner(private val source: CharSequence) : Iterator<TurtleToken> {
    private var index = 0
    private var line = 1
    private var column = 1
    private var emittedEof = false

    override fun hasNext() = !emittedEof

    override fun next(): TurtleToken {
        if (emittedEof) throw NoSuchElementException()
        if (index == source.length) {
            emittedEof = true
            return TurtleToken(TurtleTokenType.EOF, index, index, line, column)
        }
        val start = index
        val tokenLine = line
        val tokenColumn = column
        val type = when (peek()) {
            ' ', '\t', '\n', '\r' -> { consumeWhitespace(); TurtleTokenType.WHITESPACE }
            '#' -> { advance(); while (index < source.length && peek() != '\n' && peek() != '\r') advance(); TurtleTokenType.COMMENT }
            '<' -> scanIri()
            '\'', '"' -> scanString()
            '@' -> scanAtWord()
            '^' -> if (peek(1) == '^') { advance(); advance(); TurtleTokenType.DOUBLE_CARET } else { advance(); TurtleTokenType.ERROR }
            ';' -> punctuation(TurtleTokenType.SEMICOLON)
            ',' -> punctuation(TurtleTokenType.COMMA)
            '.' -> if (peek(1)?.isDigit() == true) scanNumber() else punctuation(TurtleTokenType.DOT)
            '[' -> punctuation(TurtleTokenType.OPEN_BRACKET)
            ']' -> punctuation(TurtleTokenType.CLOSE_BRACKET)
            '(' -> punctuation(TurtleTokenType.OPEN_PAREN)
            ')' -> punctuation(TurtleTokenType.CLOSE_PAREN)
            '{' -> punctuation(TurtleTokenType.OPEN_BRACE)
            '}' -> punctuation(TurtleTokenType.CLOSE_BRACE)
            '_' -> if (peek(1) == ':') scanBlankNode() else scanWordOrError()
            '+', '-' -> if (peek(1)?.isDigit() == true || (peek(1) == '.' && peek(2)?.isDigit() == true)) scanNumber() else scanWordOrError()
            else -> if (peek()?.isDigit() == true) scanNumber() else scanWordOrError()
        }
        return TurtleToken(type, start, index, tokenLine, tokenColumn)
    }

    private fun punctuation(type: TurtleTokenType): TurtleTokenType { advance(); return type }
    private fun consumeWhitespace() { while (index < source.length && peek()?.let { it in " \t\n\r" } == true) advance() }

    private fun scanIri(): TurtleTokenType {
        advance() // <
        var valid = true
        while (index < source.length && peek() != '>') {
            val c = peek()
            // A bare line break terminates an IRIREF. Leave it for whitespace scanning
            // so highlighting can recover at the beginning of the next line.
            if (c == '\n' || c == '\r') return TurtleTokenType.ERROR
            if (c == ' ' || c == '\t') valid = false
            if (c == '\\') {
                advance()
                val digits = when (peek()) { 'u' -> 4; 'U' -> 8; else -> 0 }
                if (digits == 0) valid = false else {
                    advance()
                    repeat(digits) {
                        if (peek()?.digitToIntOrNull(16) == null) valid = false
                        if (index < source.length) advance()
                    }
                }
                continue
            }
            if (c == '\n' || c == '\r' || c == '<' || c == '"' || c == '{' || c == '}' || c == '|' || c == '^' || c == '`' || c == '\\') valid = false
            if (c == '\\') {
                advance()
                val digits = when (peek()) { 'u' -> 4; 'U' -> 8; else -> 0 }
                if (digits == 0) valid = false else {
                    advance()
                    repeat(digits) {
                        if (peek()?.digitToIntOrNull(16) == null) valid = false
                        if (index < source.length) advance()
                    }
                }
            } else advance()
        }
        if (index == source.length) return TurtleTokenType.ERROR
        advance()
        return if (valid) TurtleTokenType.IRI else TurtleTokenType.ERROR
    }

    private fun scanString(): TurtleTokenType {
        val quote = peek()
        val long = peek(1) == quote && peek(2) == quote
        if (long) { advance(); advance(); advance() } else advance()
        var valid = true
        while (index < source.length) {
            if (peek() == '\\') {
                if (!scanStringEscape()) valid = false
                continue
            }
            if (!long && (peek() == '\n' || peek() == '\r')) { valid = false; break }
            if (peek() == quote && (!long || (peek(1) == quote && peek(2) == quote))) {
                if (long) { advance(); advance(); advance() } else advance()
                return if (valid) if (long) TurtleTokenType.LONG_STRING else TurtleTokenType.STRING else TurtleTokenType.ERROR
            }
            advance()
        }
        return TurtleTokenType.ERROR
    }

    private fun scanAtWord(): TurtleTokenType {
        advance()
        val wordStart = index
        while (index < source.length && (peek()?.isLetterOrDigit() == true || peek() == '-')) advance()
        val word = source.subSequence(wordStart, index).toString()
        return when {
            word == "prefix" && isBoundary() -> TurtleTokenType.PREFIX_DIRECTIVE
            word == "base" && isBoundary() -> TurtleTokenType.BASE_DIRECTIVE
            word.isNotEmpty() && word[0].isLetter() && !word.endsWith('-') && word.split('-').all { it.isNotEmpty() && it.all(Char::isLetterOrDigit) } -> TurtleTokenType.LANGUAGE_TAG
            else -> TurtleTokenType.ERROR
        }
    }

    private fun scanBlankNode(): TurtleTokenType {
        advance(); advance()
        val labelStart = index
        while (index < source.length && peek()?.let(::isNameChar) == true) advance()
        return if (index > labelStart && source[index - 1] != '.') TurtleTokenType.BLANK_NODE_LABEL else TurtleTokenType.ERROR
    }

    private fun scanNumber(): TurtleTokenType {
        if (peek() == '+' || peek() == '-') advance()
        var before = 0
        while (peek()?.isDigit() == true) { advance(); before++ }
        var decimal = false
        var after = 0
        if (peek() == '.') { decimal = true; advance(); while (peek()?.isDigit() == true) { advance(); after++ } }
        var exponent = false
        if (peek() == 'e' || peek() == 'E') {
            exponent = true; advance(); if (peek() == '+' || peek() == '-') advance()
            val expStart = index; while (peek()?.isDigit() == true) advance()
            if (index == expStart) { consumeMalformedWord(); return TurtleTokenType.ERROR }
        }
        if ((before == 0 && after == 0) || (decimal && before == 0 && after == 0) || isNameStart(peek())) { consumeMalformedWord(); return TurtleTokenType.ERROR }
        return if (exponent) TurtleTokenType.DOUBLE else if (decimal) TurtleTokenType.DECIMAL else TurtleTokenType.INTEGER
    }

    private fun scanWordOrError(): TurtleTokenType {
        val start = index
        while (index < source.length) {
            val current = peek() ?: break
            if (isDelimiter(current) && current != '.') break
            if (current == '.' && !hasLocalCharacterAfterDot()) break
            advance()
        }
        if (index == start) { advance(); return TurtleTokenType.ERROR }
        val word = source.subSequence(start, index).toString()
        if (word == "a") return TurtleTokenType.KEYWORD_A
        if (word == "true" || word == "false") return TurtleTokenType.BOOLEAN
        if (word == "PREFIX") return TurtleTokenType.SPARQL_PREFIX
        if (word == "BASE") return TurtleTokenType.SPARQL_BASE
        val colon = word.indexOf(':')
        if (colon >= 0 && validPrefixName(word, colon)) return TurtleTokenType.PREFIX_NAME
        return TurtleTokenType.ERROR
    }

    private fun validPrefixName(word: String, colon: Int): Boolean {
        val prefix = word.substring(0, colon)
        val local = word.substring(colon + 1)
        return (prefix.isEmpty() || prefix.all(::isPrefixChar)) &&
            (local.isEmpty() || (local.last() != '.' && local.all(::isLocalNameChar)))
    }
    /** Consumes one Turtle ECHAR or UCHAR after its leading backslash. */
    private fun scanStringEscape(): Boolean {
        advance()
        return when (val escaped = peek()) {
            't', 'b', 'n', 'r', 'f', '"', '\'', '\\' -> { advance(); true }
            'u', 'U' -> {
                val digits = if (escaped == 'u') 4 else 8
                advance()
                var valid = true
                repeat(digits) {
                    if (peek()?.digitToIntOrNull(16) == null) valid = false
                    if (index < source.length) advance()
                }
                valid
            }
            else -> {
                if (escaped != null) advance()
                false
            }
        }
    }
    private fun consumeMalformedWord() { while (index < source.length && !isDelimiter(peek())) advance() }
    /** A dot belongs to a prefixed-name local part only when a local character follows it. */
    private fun hasLocalCharacterAfterDot(): Boolean {
        var offset = 1
        while (peek(offset) == '.') offset++
        val following = peek(offset)
        return following != null && !isDelimiter(following)
    }
    private fun isBoundary() = index == source.length || isDelimiter(peek())
    private fun isDelimiter(c: Char?) = c == null || c.isWhitespace() || c in "#<>\"'@^;,.[](){}"
    private fun isPrefixChar(c: Char) = c.isLetterOrDigit() || c == '_' || c == '-'
    private fun isNameChar(c: Char) = c.isLetterOrDigit() || c == '_' || c == '-' || c == '.'
    private fun isLocalNameChar(c: Char) = isNameChar(c) || c == ':'
    private fun isNameStart(c: Char?): Boolean = c != null && (c.isLetter() || c == '_')
    private fun peek(offset: Int = 0): Char? = source.getOrNull(index + offset)
    private fun advance() { val c = source[index++]; if (c == '\n') { line++; column = 1 } else column++ }
}
