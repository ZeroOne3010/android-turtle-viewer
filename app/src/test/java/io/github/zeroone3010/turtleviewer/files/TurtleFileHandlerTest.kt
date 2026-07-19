package io.github.zeroone3010.turtleviewer.files

import android.net.Uri
import io.github.zeroone3010.turtleviewer.model.OpenedFile
import io.github.zeroone3010.turtleviewer.model.ViewerContent
import org.junit.Assert.*
import org.junit.Test

class TurtleFileHandlerTest {
    private val file = OpenedFile(Uri.parse("content://provider/example.ttl"), "example.ttl", null, null)
    private fun handler(bytes: ByteArray = "@prefix : <x>.".toByteArray()) = TurtleFileHandler(object : FileBytesReader {
        override fun readBytes(file: OpenedFile, maxBytes: Long) = bytes
    })

    @Test fun `recognizes documented turtle MIME types`() {
        listOf("text/turtle", "application/x-turtle", "application/turtle", "TEXT/TURTLE; charset=utf-8").forEach {
            assertTrue(TurtleFileHandler.isTurtleMimeType(it))
        }
    }
    @Test fun `recognizes ttl extension regardless of case`() { assertTrue(TurtleFileHandler.hasTurtleExtension("GRAPH.TTL")) }
    @Test fun `rejects unrelated filename`() { assertFalse(TurtleFileHandler.hasTurtleExtension("notes.txt")) }
    @Test fun `registry selects turtle handler for extension fallback`() { assertNotNull(FileHandlerRegistry(listOf(handler())).handlerFor(file)) }
    @Test fun `loads UTF-8 text`() {
        val content = handler("café".toByteArray(Charsets.UTF_8)).load(file)
        assertEquals(ViewerContent.Text("café"), content)
    }
    @Test fun `reports oversized files`() {
        val oversized = TurtleFileHandler(object : FileBytesReader { override fun readBytes(file: OpenedFile, maxBytes: Long): ByteArray { throw FileTooLargeException(maxBytes + 1, maxBytes) } })
        assertTrue((oversized.load(file) as ViewerContent.Error).message.contains("5 MB"))
    }
}
