package io.github.zeroone3010.turtleviewer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TurtleViewerApp(viewModel: ViewerViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.open(context, it) }
    }
    MaterialTheme {
        ViewerScreen(
            state = state,
            onOpenFile = { picker.launch(supportedDocumentMimeTypes) }
        )
    }
}


private val supportedDocumentMimeTypes = arrayOf(
    "text/turtle",
    "application/x-turtle",
    "application/turtle",
    "application/gpx+xml",
    "application/gpx"
)
