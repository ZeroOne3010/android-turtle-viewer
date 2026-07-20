package io.github.zeroone3010.turtleviewer.lexer

/** Convenience facade for callers that want all tokens at once. */
class TurtleLexer(private val source: CharSequence) {
    fun tokenize(): List<TurtleToken> = TurtleScanner(source).asSequence().toList()
}
