package io.github.zeroone3010.turtleviewer.files

import android.net.Uri
import io.github.zeroone3010.turtleviewer.model.OpenedFile
import io.github.zeroone3010.turtleviewer.model.ViewerContent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JsonFileHandlerTest {
    @Test fun `recognizes JSON MIME types and suffixes`() {
        assertTrue(JsonFileHandler.isJsonMimeType("application/json; charset=utf-8"))
        assertTrue(JsonFileHandler.isJsonMimeType("application/activity+json"))
        assertTrue(JsonFileHandler.isJsonMimeType("TEXT/JSON"))
        assertFalse(JsonFileHandler.isJsonMimeType("text/plain"))
    }

    @Test fun `recognizes JSON extension regardless of case`() {
        assertTrue(JsonFileHandler.hasJsonExtension("export.JSON"))
    }

    @Test fun `loads UTF-8 JSON with the larger JSON limit`() = runTest {
        var requestedLimit = 0L
        val handler = JsonFileHandler(object : FileBytesReader {
            override fun readBytes(file: OpenedFile, maxBytes: Long): ByteArray {
                requestedLimit = maxBytes
                return "{\"name\":\"café\"}".toByteArray(Charsets.UTF_8)
            }
        })
        val file = OpenedFile(Uri.parse("content://test/data.json"), "data.json", null, null)

        assertEquals(ViewerContent.Text("{\"name\":\"café\"}"), handler.load(file))
        assertEquals(JsonFileHandler.MAX_JSON_FILE_SIZE_BYTES, requestedLimit)
    }
}
