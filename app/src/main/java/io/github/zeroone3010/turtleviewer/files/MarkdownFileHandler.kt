package io.github.zeroone3010.turtleviewer.files

import io.github.zeroone3010.turtleviewer.model.OpenedFile
import io.github.zeroone3010.turtleviewer.model.ViewerContent

/** Loads Markdown as UTF-8; rendering is kept in the UI so the original source stays available. */
class MarkdownFileHandler(private val reader: FileBytesReader) : FileHandler {
    override fun canHandle(file: OpenedFile): Boolean =
        isMarkdownMimeType(file.mimeType) || hasMarkdownExtension(file.displayName)

    override suspend fun load(file: OpenedFile): ViewerContent = loadUtf8Text(reader, file)

    companion object {
        private val markdownMimeTypes = setOf(
            "text/markdown",
            "text/x-markdown",
            "application/markdown",
            "application/x-markdown"
        )

        fun isMarkdownMimeType(mimeType: String?): Boolean =
            mimeType?.substringBefore(';')?.trim()?.lowercase() in markdownMimeTypes

        fun hasMarkdownExtension(fileName: String?): Boolean =
            fileName?.substringAfterLast('.', missingDelimiterValue = "")
                ?.lowercase() in setOf("md", "markdown", "mdown", "mkd")
    }
}
