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
class MarkdownFileHandlerTest {
    @Test fun `recognizes Markdown MIME types`() {
        assertTrue(MarkdownFileHandler.isMarkdownMimeType("text/markdown; charset=utf-8"))
        assertTrue(MarkdownFileHandler.isMarkdownMimeType("APPLICATION/X-MARKDOWN"))
        assertFalse(MarkdownFileHandler.isMarkdownMimeType("text/plain"))
    }

    @Test fun `recognizes common Markdown extensions regardless of case`() {
        assertTrue(MarkdownFileHandler.hasMarkdownExtension("README.MD"))
        assertTrue(MarkdownFileHandler.hasMarkdownExtension("notes.markdown"))
        assertFalse(MarkdownFileHandler.hasMarkdownExtension("notes.txt"))
    }

    @Test fun `loads Markdown as UTF-8 text`() = runTest {
        val handler = MarkdownFileHandler(object : FileBytesReader {
            override fun readBytes(file: OpenedFile, maxBytes: Long) =
                "# Héllo\n\n**world**".toByteArray(Charsets.UTF_8)
        })
        val file = OpenedFile(Uri.parse("content://test/readme.md"), "README.md", null, null)

        assertEquals(ViewerContent.Text("# Héllo\n\n**world**"), handler.load(file))
    }
}
