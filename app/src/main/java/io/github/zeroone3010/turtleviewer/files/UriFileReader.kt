package io.github.zeroone3010.turtleviewer.files

import android.content.ContentResolver
import android.provider.OpenableColumns
import io.github.zeroone3010.turtleviewer.model.OpenedFile
import java.io.ByteArrayOutputStream
import java.io.IOException

interface FileBytesReader {
    fun readBytes(file: OpenedFile, maxBytes: Long): ByteArray
}

class UriFileReader(private val resolver: ContentResolver) : FileBytesReader {
    fun metadata(uri: android.net.Uri, suppliedMimeType: String? = null): OpenedFile {
        var name: String? = null
        var size: Long? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        return OpenedFile(uri, name, suppliedMimeType ?: resolver.getType(uri), size)
    }

    @Throws(IOException::class)
    override fun readBytes(file: OpenedFile, maxBytes: Long): ByteArray {
        file.sizeBytes?.let { if (it > maxBytes) throw FileTooLargeException(it, maxBytes) }
        resolver.openInputStream(file.uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var totalBytes = 0L
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                totalBytes += count
                if (totalBytes > maxBytes) throw FileTooLargeException(totalBytes, maxBytes)
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        } ?: throw IOException("The selected provider did not provide file contents.")
    }
}

class FileTooLargeException(val actualBytes: Long, val maxBytes: Long) : IOException()
