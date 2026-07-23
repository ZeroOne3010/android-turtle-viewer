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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.zeroone3010.turtleviewer.model.ViewerContent
import io.github.zeroone3010.turtleviewer.rdf.*
import io.github.zeroone3010.turtleviewer.gpx.GpxDisplayItem
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(state: ViewerUiState, onOpenFile: () -> Unit) {
    var monospace by rememberSaveable { mutableStateOf(true) }
    var wrapLines by rememberSaveable { mutableStateOf(false) }
    var showWhitespace by rememberSaveable { mutableStateOf(false) }
    var darkMode by rememberSaveable { mutableStateOf(false) }
    var fontSize by rememberSaveable { mutableIntStateOf(DefaultFontSizeSp) }
    // GPX starts on its sampled readable view, so the full XML source is not composed first.
    // Unlike a derived loading flag, this remains user-controlled after the initial selection.
    var readableTab by rememberSaveable(state.file?.uri) { mutableStateOf(state.readableGpx != null) }
    val hasReadable = state.readableRdf != null || state.readableGpx != null
    LaunchedEffect(state.readableRdf) {
        if (
            state.readableRdf is ReadableRdfState.Ready ||
            state.readableRdf is ReadableRdfState.Empty
        ) {
            readableTab = true
        }
    }
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
                        if (hasReadable) TabRow(selectedTabIndex = if (readableTab) 0 else 1) {
                            Tab(readableTab, { readableTab = true }, text = { Text("Readable") })
                            Tab(!readableTab, { readableTab = false }, text = { Text("Source") })
                        }
                        if (!readableTab || !hasReadable) Row(
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
                            readableTab && state.readableGpx != null -> GpxReadableContent(state.readableGpx, Modifier.weight(1f))
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

@Composable private fun GpxReadableContent(state: ReadableGpxState, modifier: Modifier) = when (state) {
    ReadableGpxState.Loading -> Box(modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
    is ReadableGpxState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, modifier = modifier)
    is ReadableGpxState.Ready -> LazyColumn(modifier.fillMaxWidth()) {
        items(state.items.size, key = { it }) { index ->
            when (val item = state.items[index]) {
                is GpxDisplayItem.TrackHeading -> Text("Track ${item.number}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                is GpxDisplayItem.SegmentHeading -> Text(
                    "Segment ${item.number} · ${item.pointCount} points" +
                        if (item.displayedPointCount < item.pointCount) " · showing ${item.displayedPointCount} samples" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                is GpxDisplayItem.Point -> Text(gpxPointAnnotatedString(item), style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp))
            }
        }
    }
}

/** Produces one compact, accessible text node while preserving semantic colors in spans. */
@Composable internal fun gpxPointAnnotatedString(point: GpxDisplayItem.Point): AnnotatedString {
    val colors = MaterialTheme.colorScheme
    return AnnotatedString.Builder().apply {
        withStyle(SpanStyle(color = colors.onSurfaceVariant)) { append(point.timestamp) }
        withStyle(SpanStyle(color = colors.outline)) { append(" · ") }
        withStyle(SpanStyle(color = colors.onSurface)) { append(point.coordinates) }
        append("\n")
        if (point.isStart) withStyle(SpanStyle(color = colors.outline, fontStyle = FontStyle.Italic)) { append("Start") }
        else {
            withStyle(SpanStyle(color = colors.primary)) { append(point.speed ?: "—") }
            withStyle(SpanStyle(color = colors.outline)) { append(" · ") }
            withStyle(SpanStyle(color = colors.secondary)) { append(point.bearing ?: "—") }
        }
        point.point.elevation?.let { elevation ->
            withStyle(SpanStyle(color = colors.outline)) { append(" · ") }
            withStyle(SpanStyle(color = colors.onSurfaceVariant)) { append(String.format(Locale.US, "%.1f m", elevation)) }
        }
    }.toAnnotatedString()
}

@Composable private fun ReadableContent(state: ReadableRdfState, onSource: () -> Unit, modifier: Modifier) = when (state) {
    ReadableRdfState.Loading -> Box(modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator(); Text("Loading readable outline") }
    ReadableRdfState.Empty -> Box(modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Empty graph") }
    is ReadableRdfState.Error -> ReadableError(state, onSource, modifier)
    is ReadableRdfState.Ready -> LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.document.roots, key = { it.id }) { ResourceOutline(it, resources = state.document.resources) }
        if (state.document.otherResources.isNotEmpty()) item { OtherResources(state.document.otherResources, state.document.resources) }
    }
}

@Composable private fun ReadableError(state: ReadableRdfState.Error, onSource: () -> Unit, modifier: Modifier) {
    var detailsVisible by rememberSaveable(state.technicalDetails) { mutableStateOf(false) }
    Column(modifier.verticalScroll(rememberScrollState())) {
        Text(state.message, color = MaterialTheme.colorScheme.error)
        Text("Check the source for invalid Turtle syntax, or expand the diagnostic details below.", style = MaterialTheme.typography.bodySmall)
        if (state.technicalDetails != null) {
            TextButton(onClick = { detailsVisible = !detailsVisible }) { Text(if (detailsVisible) "Hide diagnostic details" else "Show diagnostic details") }
            if (detailsVisible) {
                SelectionContainer {
                    Text(
                        state.technicalDetails,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        Button(onClick = onSource) { Text("View Source") }
    }
}

@Composable private fun OtherResources(resources: List<RdfResourceView>, index: Map<String, RdfResourceView>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = !expanded }) { Text("Other resources · ${resources.size}") }
    if (expanded) resources.forEach { ResourceOutline(it, resources = index) }
}

@Composable private fun ResourceOutline(resource: RdfResourceView, depth: Int = 0, path: Set<String> = emptySet(), resources: Map<String, RdfResourceView>, resourceDepth: Int = 0) {
    Column(Modifier.padding(start = (depth * 12).dp).fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                resource.displayLabel,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            DetailsButton("resource ${resource.displayLabel}") { ResourceDetails(resource) }
        }
        SelectionContainer { Text(resource.compactId, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(8.dp))
        resource.properties.forEach { PropertyOutline(it, path + resource.id, resources, resourceDepth) }
    }
}

/** A predicate and its object are deliberately one visual unit, not separate RDF-debugger rows. */
@Composable private fun PropertyOutline(property: RdfPropertyView, path: Set<String>, resources: Map<String, RdfResourceView>, resourceDepth: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(property.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            DetailsButton("predicate ${property.label}") { PredicateDetails(property) }
        }
        property.values.forEachIndexed { index, value ->
            val child = (value as? RdfValueView.ResourceReference)?.let { resources[it.resourceId] }
            if (child != null && value.resourceId !in path && resourceDepth < 20) {
                NestedResourceValue(property, child, path, resources, resourceDepth, index)
            } else {
                InlineValue(value, property.label, index)
                if (value is RdfValueView.ResourceReference && value.resourceId in path) {
                    Text("Already shown in this branch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (property.values.size > 1) Text("${property.values.size} values", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun NestedResourceValue(property: RdfPropertyView, child: RdfResourceView, path: Set<String>, resources: Map<String, RdfResourceView>, resourceDepth: Int, index: Int) {
    var expanded by rememberSaveable(property.iri, child.id, index) { mutableStateOf(false) }
    Card(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().padding(top = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(child.displayLabel, style = MaterialTheme.typography.bodyLarge)
                Text(if (expanded) "⌃" else "›", style = MaterialTheme.typography.titleMedium)
            }
            Text("${child.properties.size} properties", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                child.properties.forEach { PropertyOutline(it, path + child.id, resources, resourceDepth + 1) }
            }
        }
    }
}

@Composable private fun InlineValue(value: RdfValueView, propertyLabel: String, index: Int) {
    when (value) {
        is RdfValueView.LiteralValue -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SelectionContainer { Text(value.displayValue, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)) }
            DetailsButton("value for $propertyLabel ${index + 1}") { LiteralDetails(value) }
        }
        is RdfValueView.ResourceReference -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(value.displayLabel, style = MaterialTheme.typography.bodyLarge)
            DetailsButton("value for $propertyLabel ${index + 1}") { ResourceReferenceDetails(value) }
        }
    }
}

@Composable private fun DetailsButton(description: String, details: @Composable () -> Unit) {
    var visible by rememberSaveable(description) { mutableStateOf(false) }
    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
        TextButton(onClick = { visible = !visible }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp), modifier = Modifier.semantics { contentDescription = "Show technical details for $description" }) { Text("ⓘ") }
        if (visible) Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.small) {
            SelectionContainer { Column(Modifier.padding(8.dp)) { details() } }
        }
    }
}

@Composable private fun ResourceDetails(resource: RdfResourceView) { Text("Identifier\n${resource.id}\n\nResource kind\n${resource.kind}", style = MaterialTheme.typography.bodySmall) }
@Composable private fun ResourceReferenceDetails(value: RdfValueView.ResourceReference) { Text("Value\n${value.resourceId}\n\nResource kind\n${value.kind}", style = MaterialTheme.typography.bodySmall) }
@Composable private fun PredicateDetails(property: RdfPropertyView) { Text("Predicate\n${property.iri}", style = MaterialTheme.typography.bodySmall) }
@Composable private fun LiteralDetails(value: RdfValueView.LiteralValue) { Text("Lexical value\n${value.lexicalValue}\n\nDatatype\n${value.datatypeIri ?: "none"}\n\nLanguage\n${value.language ?: "none"}", style = MaterialTheme.typography.bodySmall) }

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
