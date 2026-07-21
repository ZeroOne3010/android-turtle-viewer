package io.github.zeroone3010.turtleviewer.files

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxFileHandlerTest {
    @Test fun `recognizes official GPX MIME types including parameters`() {
        assertTrue(GpxFileHandler.isGpxMimeType("application/gpx+xml; charset=utf-8"))
        assertTrue(GpxFileHandler.isGpxMimeType("APPLICATION/GPX"))
    }

    @Test fun `recognizes gpx extension regardless of case`() { assertTrue(GpxFileHandler.hasGpxExtension("ride.GPX")) }
    @Test fun `does not claim arbitrary XML files`() { assertFalse(GpxFileHandler.isGpxMimeType("application/xml")) }
}
