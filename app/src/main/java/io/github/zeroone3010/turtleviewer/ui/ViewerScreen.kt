package io.github.zeroone3010.turtleviewer.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import io.github.zeroone3010.turtleviewer.rdf.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(state: ViewerUiState, onOpenFile: () -> Unit) {
    var monospace by rememberSaveable { mutableStateOf(true) }
    var wrapLines by rememberSaveable { mutableStateOf(false) }
    var showWhitespace by rememberSaveable { mutableStateOf(false) }
    var darkMode by rememberSaveable { mutableStateOf(false) }
    var fontSize by rememberSaveable { mutableIntStateOf(DefaultFontSizeSp) }
    var readableTab by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.readableRdf) { if (state.readableRdf is ReadableRdfState.Ready || state.readableRdf is ReadableRdfState.Empty) readableTab = true }
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
                        if (state.readableRdf != null) TabRow(selectedTabIndex = if (readableTab) 0 else 1) {
                            Tab(readableTab, { readableTab = true }, text = { Text("Readable") })
                            Tab(!readableTab, { readableTab = false }, text = { Text("Source") })
                        }
                        if (!readableTab || state.readableRdf == null) Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp)
                        ) {
                            FilterChip(selected = monospace, onClick = { monospace = !monospace }, label = { Text("Monospace") })
                            FilterChip(selected = wrapLines, onClick = { wrapLines = !wrapLines }, label = { Text("Wrap lines") })
                            FilterChip(selected = showWhitespace, onClick = { showWhitespace = !showWhitespace }, label = { Text("Show whitespace") })
                            FilterChip(selected = darkMode, onClick = { darkMode = !darkMode }, label = { Text("Dark mode") })
                            OutlinedButton(
                                onClick = { fontSize = (fontSize - FontSizeStepSp).coerceAtLeast(MinFontSizeSp) },
                                enabled = fontSize > MinFontSizeSp,
                                modifier = Modifier.semantics { contentDescription = "Decrease font size" }
                            ) { Text("A−") }
                            OutlinedButton(
                                onClick = { fontSize = (fontSize + FontSizeStepSp).coerceAtMost(MaxFontSizeSp) },
                                enabled = fontSize < MaxFontSizeSp,
                                modifier = Modifier.semantics { contentDescription = "Increase font size" }
                            ) { Text("A+") }
                        }
                        when {
                            state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
                            readableTab && state.readableRdf != null -> ReadableContent(state.readableRdf, { readableTab = false }, Modifier.weight(1f))
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

@Composable private fun ReadableContent(state: ReadableRdfState, onSource: () -> Unit, modifier: Modifier) = when (state) {
    ReadableRdfState.Loading -> Box(modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator(); Text("Loading readable outline") }
    ReadableRdfState.Empty -> Box(modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Empty graph") }
    is ReadableRdfState.Error -> Column(modifier) { Text(state.message, color = MaterialTheme.colorScheme.error); Button(onClick = onSource) { Text("View Source") } }
    is ReadableRdfState.Ready -> LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.document.roots, key = { it.id }) { ResourceOutline(it, resources = state.document.resources) }
        if (state.document.otherResources.isNotEmpty()) item { OtherResources(state.document.otherResources, state.document.resources) }
    }
}

@Composable private fun OtherResources(resources: List<RdfResourceView>, index: Map<String, RdfResourceView>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = !expanded }) { Text("Other resources (${resources.size})") }
    if (expanded) resources.forEach { ResourceOutline(it, resources = index) }
}

@Composable private fun ResourceOutline(resource: RdfResourceView, depth: Int = 0, path: Set<String> = emptySet(), resources: Map<String, RdfResourceView>) {
    var expanded by rememberSaveable(resource.id, depth) { mutableStateOf(depth == 0) }
    var details by rememberSaveable(resource.id + "details") { mutableStateOf(false) }
    Column(Modifier.padding(start = (depth * 12).dp).fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.semantics { contentDescription = "Toggle ${resource.displayLabel}" }) { Text(resource.displayLabel) }
        Text(resource.compactId, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { details = !details }) { Text("Technical details") }
        if (details) SelectionContainer { Text("Identifier: ${resource.id}\nResource kind: ${resource.kind}", style = MaterialTheme.typography.bodySmall) }
        if (expanded) resource.properties.forEach { PropertyOutline(it, depth, path + resource.id, resources) }
    }
}

@Composable private fun PropertyOutline(property: RdfPropertyView, depth: Int, path: Set<String>, resources: Map<String, RdfResourceView>) {
    var expanded by rememberSaveable(property.iri + depth) { mutableStateOf(true) }; var details by rememberSaveable(property.iri + "details") { mutableStateOf(false) }
    Column(Modifier.padding(start = ((depth + 1) * 12).dp)) {
        TextButton(onClick = { expanded = !expanded }) { Text("${property.label} (${property.values.size})") }
        TextButton(onClick = { details = !details }) { Text("Technical details") }
        if (details) SelectionContainer { Text("Compact IRI: ${property.compactIri}\nFull IRI: ${property.iri}", style = MaterialTheme.typography.bodySmall) }
        if (expanded) property.values.forEach { value -> when (value) {
            is RdfValueView.LiteralValue -> LiteralOutline(value, depth + 2)
            is RdfValueView.ResourceReference -> {
                val child = resources[value.resourceId]
                if (child != null && value.resourceId !in path && depth < 20) ResourceOutline(child, depth + 2, path, resources)
                else SelectionContainer { Text("${value.displayLabel}${if (value.resourceId in path) "\n↩ already shown in this branch" else ""}", modifier = Modifier.padding(start = ((depth + 2) * 12).dp)) }
            }
        } }
    }
}

@Composable private fun LiteralOutline(value: RdfValueView.LiteralValue, depth: Int) {
    var details by rememberSaveable(value.lexicalValue + depth) { mutableStateOf(false) }
    Column(Modifier.padding(start = (depth * 12).dp)) { SelectionContainer { Text(value.displayValue) }; TextButton(onClick = { details = !details }) { Text("Technical details") }; if (details) SelectionContainer { Text("Lexical value: ${value.lexicalValue}\nDatatype: ${value.datatypeIri ?: "none"}\nLanguage: ${value.language ?: "none"}", style = MaterialTheme.typography.bodySmall) } }
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
