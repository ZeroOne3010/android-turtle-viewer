package io.github.zeroone3010.turtleviewer.files

import io.github.zeroone3010.turtleviewer.model.OpenedFile
import io.github.zeroone3010.turtleviewer.model.ViewerContent

class TurtleFileHandler(private val reader: FileBytesReader) : FileHandler {
    override fun canHandle(file: OpenedFile): Boolean = isTurtleMimeType(file.mimeType) || hasTurtleExtension(file.displayName)

    override suspend fun load(file: OpenedFile): ViewerContent = loadUtf8Text(reader, file)

    companion object {
        const val MAX_FILE_SIZE_BYTES = MAX_TEXT_FILE_SIZE_BYTES
        private val turtleMimeTypes = setOf("text/turtle", "application/x-turtle", "application/turtle")
        fun isTurtleMimeType(mimeType: String?): Boolean = mimeType?.substringBefore(';')?.lowercase() in turtleMimeTypes
        fun hasTurtleExtension(fileName: String?): Boolean = fileName?.endsWith(".ttl", ignoreCase = true) == true
    }
}
