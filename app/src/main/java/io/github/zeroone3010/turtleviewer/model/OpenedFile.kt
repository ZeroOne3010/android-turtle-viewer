package io.github.zeroone3010.turtleviewer.model

import android.net.Uri

/** URI and lightweight metadata, intentionally independent of a particular file format. */
data class OpenedFile(
    val uri: Uri,
    val displayName: String?,
    val mimeType: String?,
    val sizeBytes: Long?
)
