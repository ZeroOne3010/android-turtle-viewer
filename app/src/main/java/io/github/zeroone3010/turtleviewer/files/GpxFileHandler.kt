package io.github.zeroone3010.turtleviewer.files

import io.github.zeroone3010.turtleviewer.model.OpenedFile
import io.github.zeroone3010.turtleviewer.model.ViewerContent
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import kotlin.text.Charsets.UTF_8

/** Loads GPX, an XML-based GPS exchange format, as UTF-8 text. */
class GpxFileHandler(private val reader: FileBytesReader) : FileHandler {
    override fun canHandle(file: OpenedFile): Boolean = isGpxMimeType(file.mimeType) || hasGpxExtension(file.displayName)

    override suspend fun load(file: OpenedFile): ViewerContent = loadUtf8Text(reader, file)

    companion object {
        private val gpxMimeTypes = setOf("application/gpx+xml", "application/gpx")
        fun isGpxMimeType(mimeType: String?): Boolean = mimeType?.substringBefore(';')?.lowercase() in gpxMimeTypes
        fun hasGpxExtension(fileName: String?): Boolean = fileName?.endsWith(".gpx", ignoreCase = true) == true
    }
}

internal fun loadUtf8Text(reader: FileBytesReader, file: OpenedFile): ViewerContent = try {
    val bytes = reader.readBytes(file, MAX_TEXT_FILE_SIZE_BYTES)
    val text = UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    ViewerContent.Text(text)
} catch (error: FileTooLargeException) {
    ViewerContent.Error("This file is larger than the 5 MB viewing limit.")
} catch (error: CharacterCodingException) {
    ViewerContent.Error("This file is not valid UTF-8 text and cannot be displayed.")
} catch (error: Exception) {
    ViewerContent.Error("Unable to open this file: ${error.message ?: "unknown error"}")
}

internal const val MAX_TEXT_FILE_SIZE_BYTES = 5L * 1024 * 1024
