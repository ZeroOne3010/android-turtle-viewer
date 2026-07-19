package io.github.zeroone3010.turtleviewer.files

import android.content.Intent
import android.net.Uri

/** Keeps Android intent extraction separate from metadata lookup and loading. */
fun incomingFileUri(intent: Intent?): Uri? = when (intent?.action) {
    Intent.ACTION_VIEW -> intent.data
    Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
    else -> null
}
