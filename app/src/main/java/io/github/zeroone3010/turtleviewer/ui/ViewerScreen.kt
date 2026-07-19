package io.github.zeroone3010.turtleviewer.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.zeroone3010.turtleviewer.model.ViewerContent
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(state: ViewerUiState, onOpenFile: () -> Unit) {
    var monospace by rememberSaveable { mutableStateOf(true) }
    var wrapLines by rememberSaveable { mutableStateOf(false) }
    var showWhitespace by rememberSaveable { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Turtle Viewer") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            when {
                state.file == null && state.content == null -> EmptyState(onOpenFile)
                else -> {
                    state.file?.let { file ->
                        Text(file.displayName ?: "Unnamed Turtle file", style = MaterialTheme.typography.titleMedium)
                        file.mimeType?.let { Text("MIME type: $it", style = MaterialTheme.typography.bodySmall) }
                        file.sizeBytes?.let { Text("Size: ${formatSize(it)}", style = MaterialTheme.typography.bodySmall) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                        FilterChip(selected = monospace, onClick = { monospace = !monospace }, label = { Text("Monospace") })
                        FilterChip(selected = wrapLines, onClick = { wrapLines = !wrapLines }, label = { Text("Wrap lines") })
                        FilterChip(selected = showWhitespace, onClick = { showWhitespace = !showWhitespace }, label = { Text("Show whitespace") })
                    }
                    when {
                        state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
                        state.content is ViewerContent.Text -> TextContent((state.content as ViewerContent.Text).value, monospace, wrapLines, showWhitespace, Modifier.weight(1f))
                        state.content is ViewerContent.Error -> Text((state.content as ViewerContent.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    }
                    Button(onClick = onOpenFile, modifier = Modifier.padding(top = 12.dp)) { Text("Open another file") }
                }
            }
        }
    }
}

@Composable private fun EmptyState(onOpenFile: () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text("Open a Turtle (.ttl) file to view its raw text.")
        Button(onClick = onOpenFile, modifier = Modifier.padding(top = 16.dp)) { Text("Open file") }
    }
}

@Composable private fun TextContent(text: String, monospace: Boolean, wrap: Boolean, whitespace: Boolean, modifier: Modifier) {
    val shown = if (whitespace) text.replace(" ", "·").replace("\t", "→\t") else text
    val vertical = rememberScrollState(); val horizontal = rememberScrollState()
    SelectionContainer {
        Text(shown, fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            modifier = modifier.fillMaxWidth().verticalScroll(vertical).then(if (wrap) Modifier else Modifier.horizontalScroll(horizontal)).testTag("file-content"),
            softWrap = wrap)
    }
}
private fun formatSize(bytes: Long): String = if (bytes < 1024) "$bytes B" else String.format(Locale.US, "%.1f KB", bytes / 1024.0)
