package io.github.zeroone3010.turtleviewer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.github.zeroone3010.turtleviewer.ui.TurtleViewerApp
import io.github.zeroone3010.turtleviewer.files.incomingFileUri
import io.github.zeroone3010.turtleviewer.ui.ViewerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ViewerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TurtleViewerApp(viewModel) }
        processIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIncomingIntent(intent)
    }

    private fun processIncomingIntent(intent: Intent?) {
        incomingFileUri(intent)?.let { viewModel.open(this, it, intent?.type) }
    }
}
