package io.github.zeroone3010.turtleviewer.files

import android.content.Intent
import android.net.Uri
import android.os.Build

/** Keeps Android intent extraction separate from metadata lookup and loading. */
fun incomingFileUri(intent: Intent?): Uri? = when (intent?.action) {
    Intent.ACTION_VIEW -> intent.data
    Intent.ACTION_SEND -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
    }
    else -> null
}
