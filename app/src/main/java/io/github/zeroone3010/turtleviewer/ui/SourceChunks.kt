package io.github.zeroone3010.turtleviewer.ui

/** Bounded source pieces for a lazy source view. Build this away from the UI thread. */
data class SourceChunks(val ranges: List<IntRange>) {
    companion object {
        fun from(source: CharSequence, maxChunkLength: Int = DEFAULT_MAX_CHUNK_LENGTH): SourceChunks {
            require(maxChunkLength > 0)
            if (source.isEmpty()) return SourceChunks(listOf(0..-1))
            val ranges = ArrayList<IntRange>((source.length / maxChunkLength) + 1)
            var start = 0
            while (start < source.length) {
                var end = minOf(start + maxChunkLength, source.length)
                if (end < source.length) {
                    val newline = (end - 1 downTo start).firstOrNull { source[it] == '\n' }
                    if (newline != null && newline >= start + maxChunkLength / 2) end = newline + 1
                }
                ranges += start until end
                start = end
            }
            return SourceChunks(ranges)
        }

        private const val DEFAULT_MAX_CHUNK_LENGTH = 2_048
    }
}
