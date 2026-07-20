package io.github.zeroone3010.turtleviewer.files

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomingIntentTest {
    @Test fun extractsViewUri() {
        val uri = Uri.parse("content://example/graph.ttl")
        assertEquals(uri, incomingFileUri(Intent(Intent.ACTION_VIEW, uri)))
    }
    @Test fun extractsSharedStreamUri() {
        val uri = Uri.parse("content://example/graph.ttl")
        assertEquals(uri, incomingFileUri(Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uri)))
    }
}
