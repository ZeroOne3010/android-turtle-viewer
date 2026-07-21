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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.zeroone3010.turtleviewer.model.ViewerContent
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(state: ViewerUiState, onOpenFile: () -> Unit) {
    var monospace by rememberSaveable { mutableStateOf(true) }
    var wrapLines by rememberSaveable { mutableStateOf(false) }
    var showWhitespace by rememberSaveable { mutableStateOf(false) }
    var darkMode by rememberSaveable { mutableStateOf(false) }
    var fontSize by rememberSaveable { mutableIntStateOf(DefaultFontSizeSp) }
    MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
        Scaffold(topBar = { TopAppBar(title = { Text("Turtle Viewer") }) }) { padding ->
            Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
                when {
                    state.file == null && state.content == null -> EmptyState(onOpenFile)
                    else -> {
                        state.file?.let { file ->
                            Text(file.displayName ?: "Unnamed file", style = MaterialTheme.typography.titleMedium)
                            file.mimeType?.let { Text("MIME type: $it", style = MaterialTheme.typography.bodySmall) }
                            file.sizeBytes?.let { Text("Size: ${formatSize(it)}", style = MaterialTheme.typography.bodySmall) }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp)
                        ) {
                            FilterChip(selected = monospace, onClick = { monospace = !monospace }, label = { Text("Monospace") })
                            FilterChip(selected = wrapLines, onClick = { wrapLines = !wrapLines }, label = { Text("Wrap lines") })
                            FilterChip(selected = showWhitespace, onClick = { showWhitespace = !showWhitespace }, label = { Text("Show whitespace") })
                            FilterChip(selected = darkMode, onClick = { darkMode = !darkMode }, label = { Text("Dark mode") })
                            OutlinedButton(
                                onClick = { fontSize -= FontSizeStepSp },
                                enabled = fontSize > MinFontSizeSp,
                                modifier = Modifier.semantics { contentDescription = "Decrease font size" }
                            ) { Text("A−") }
                            OutlinedButton(
                                onClick = { fontSize += FontSizeStepSp },
                                enabled = fontSize < MaxFontSizeSp,
                                modifier = Modifier.semantics { contentDescription = "Increase font size" }
                            ) { Text("A+") }
                        }
                        when {
                            state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
                            state.content is ViewerContent.Text -> TextContent(
                                (state.content as ViewerContent.Text).value,
                                state.syntaxFormat,
                                if (darkMode) darkSyntaxColors else lightSyntaxColors,
                                monospace,
                                wrapLines,
                                showWhitespace,
                                fontSize,
                                Modifier.weight(1f)
                            )
                            state.content is ViewerContent.Error -> Text((state.content as ViewerContent.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                        }
                        Button(onClick = onOpenFile, modifier = Modifier.padding(top = 12.dp)) { Text("Open another file") }
                    }
                }
            }
        }
    }
}

@Composable private fun EmptyState(onOpenFile: () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text("Open a Turtle (.ttl) or GPX (.gpx) file to view its raw text.")
        Button(onClick = onOpenFile, modifier = Modifier.padding(top = 16.dp)) { Text("Open file") }
    }
}

@Composable private fun TextContent(
    text: String,
    syntaxFormat: SyntaxFormat?,
    syntaxColors: SyntaxColors,
    monospace: Boolean,
    wrap: Boolean,
    whitespace: Boolean,
    fontSize: Int,
    modifier: Modifier
) {
    val highlightedText = remember(text, syntaxFormat, syntaxColors) {
        syntaxFormat?.let { annotatedString(text, it, syntaxColors) } ?: AnnotatedString(text)
    }
    val displayText = highlightedText.let {
        if (whitespace) it.withVisibleWhitespace() else it
    }
    val vertical = rememberScrollState(); val horizontal = rememberScrollState()
    SelectionContainer {
        Text(displayText, fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            modifier = modifier.fillMaxWidth().verticalScroll(vertical).then(if (wrap) Modifier else Modifier.horizontalScroll(horizontal)).testTag("file-content"),
            softWrap = wrap,
            fontSize = fontSize.sp)
    }
}

private const val MinFontSizeSp = 6
private const val DefaultFontSizeSp = 16
private const val MaxFontSizeSp = 32
private const val FontSizeStepSp = 1

/** Makes spaces and tabs visible without discarding syntax highlighting spans. */
internal fun AnnotatedString.withVisibleWhitespace(): AnnotatedString {
    val offsets = IntArray(length + 1)
    val visibleText = buildString {
        this@withVisibleWhitespace.forEachIndexed { index, character ->
            offsets[index] = length
            append(if (character == ' ') '·' else if (character == '\t') "→\t" else character)
        }
        offsets[this@withVisibleWhitespace.length] = length
    }
    return AnnotatedString.Builder(visibleText).apply {
        this@withVisibleWhitespace.spanStyles.forEach { range ->
            addStyle(range.item, offsets[range.start], offsets[range.end])
        }
        this@withVisibleWhitespace.paragraphStyles.forEach { range ->
            addStyle(range.item, offsets[range.start], offsets[range.end])
        }
    }.toAnnotatedString()
}
private fun formatSize(bytes: Long): String = if (bytes < 1024) "$bytes B" else String.format(Locale.US, "%.1f KB", bytes / 1024.0)
