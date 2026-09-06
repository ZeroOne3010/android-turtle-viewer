package io.github.zeroone3010.turtleviewer.json

/**
 * Pretty prints JSON in one linear pass while preserving the exact spelling of strings and
 * numbers. It does not build an object tree, which keeps large documents fast and memory use
 * predictable. Structural characters inside quoted strings are left untouched.
 */
object JsonPrettyPrinter {
    fun format(source: CharSequence, indentSize: Int = 2): String {
        require(indentSize > 0)
        val output = StringBuilder(source.length + source.length / 8)
        var depth = 0
        var quoted = false
        var escaped = false

        fun newline() {
            output.append('\n')
            repeat(depth * indentSize) { output.append(' ') }
        }

        source.forEach { character ->
            if (quoted) {
                output.append(character)
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == '"' -> quoted = false
                }
            } else {
                when (character) {
                    '"' -> { quoted = true; output.append(character) }
                    '{', '[' -> { output.append(character); depth++; newline() }
                    '}', ']' -> {
                        depth = (depth - 1).coerceAtLeast(0)
                        if (output.isNotEmpty() && output.last() == '\n') {
                            output.setLength(output.length - 1)
                            while (output.isNotEmpty() && output.last() == ' ') output.setLength(output.length - 1)
                        } else newline()
                        output.append(character)
                    }
                    ',' -> { output.append(character); newline() }
                    ':' -> output.append(": ")
                    ' ', '\t', '\r', '\n' -> Unit
                    else -> output.append(character)
                }
            }
        }
        return output.toString()
    }
}
