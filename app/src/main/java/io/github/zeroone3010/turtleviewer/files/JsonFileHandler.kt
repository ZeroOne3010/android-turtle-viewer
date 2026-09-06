package io.github.zeroone3010.turtleviewer.files

import io.github.zeroone3010.turtleviewer.model.OpenedFile
import io.github.zeroone3010.turtleviewer.model.ViewerContent

/** Loads JSON as UTF-8. Formatting is deliberately performed separately in the view model. */
class JsonFileHandler(private val reader: FileBytesReader) : FileHandler {
    override fun canHandle(file: OpenedFile): Boolean =
        isJsonMimeType(file.mimeType) || hasJsonExtension(file.displayName)

    override suspend fun load(file: OpenedFile): ViewerContent =
        loadUtf8Text(reader, file, MAX_JSON_FILE_SIZE_BYTES, "50 MB")

    companion object {
        const val MAX_JSON_FILE_SIZE_BYTES = 50L * 1024 * 1024
        fun isJsonMimeType(mimeType: String?): Boolean {
            val normalized = mimeType?.substringBefore(';')?.trim()?.lowercase()
            return normalized == "application/json" || normalized == "text/json" ||
                normalized?.endsWith("+json") == true
        }

        fun hasJsonExtension(fileName: String?): Boolean =
            fileName?.endsWith(".json", ignoreCase = true) == true
    }
}
