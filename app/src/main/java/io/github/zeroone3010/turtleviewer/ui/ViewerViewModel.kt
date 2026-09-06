package io.github.zeroone3010.turtleviewer.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.text.AnnotatedString
import io.github.zeroone3010.turtleviewer.files.FileHandlerRegistry
import io.github.zeroone3010.turtleviewer.files.GpxFileHandler
import io.github.zeroone3010.turtleviewer.files.JsonFileHandler
import io.github.zeroone3010.turtleviewer.files.TurtleFileHandler
import io.github.zeroone3010.turtleviewer.files.UriFileReader
import io.github.zeroone3010.turtleviewer.model.OpenedFile
import io.github.zeroone3010.turtleviewer.model.ViewerContent
import io.github.zeroone3010.turtleviewer.gpx.GpxDisplayItem
import io.github.zeroone3010.turtleviewer.gpx.GpxReadableParser
import io.github.zeroone3010.turtleviewer.gpx.gpxDisplayItems
import io.github.zeroone3010.turtleviewer.json.JsonPrettyPrinter
import io.github.zeroone3010.turtleviewer.rdf.ReadableRdfState
import io.github.zeroone3010.turtleviewer.rdf.RdfErrorDetails
import io.github.zeroone3010.turtleviewer.rdf.TurtleRdfParser
import org.eclipse.rdf4j.rio.RDFParseException
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class ViewerUiState(
    val file: OpenedFile? = null,
    val content: ViewerContent? = null,
    val syntaxFormat: SyntaxFormat? = null,
    val loading: Boolean = false,
    /** Source highlighting is running off the main thread while unhighlighted source remains usable. */
    val sourceLoading: Boolean = false,
    val highlightedSource: AnnotatedString? = null,
    val darkHighlightedSource: AnnotatedString? = null,
    /** Prepared off the main thread so long source documents can be rendered lazily. */
    val sourceChunks: SourceChunks? = null,
    val readableRdf: ReadableRdfState? = null,
    val readableGpx: ReadableGpxState? = null,
    val readableJson: ReadableJsonState? = null
)

sealed interface ReadableJsonState {
    data object Loading : ReadableJsonState
    data class Ready(val formatted: String, val chunks: SourceChunks) : ReadableJsonState
}

sealed interface ReadableGpxState {
    data object Loading : ReadableGpxState
    data class Ready(val items: List<GpxDisplayItem>) : ReadableGpxState
    data class Error(val message: String) : ReadableGpxState
}

class ViewerViewModel : ViewModel() {
    private val _state = MutableStateFlow(ViewerUiState())
    val state: StateFlow<ViewerUiState> = _state.asStateFlow()
    private val requestGeneration = AtomicLong(0)
    private var openJob: Job? = null

    /** Opens only the newest requested URI; slow providers cannot overwrite a newer selection. */
    fun open(context: Context, uri: Uri, intentMimeType: String? = null) {
        val requestId = requestGeneration.incrementAndGet()
        openJob?.cancel()
        openJob = viewModelScope.launch(Dispatchers.IO) {
            val reader = UriFileReader(context.contentResolver)
            val file = try {
                reader.metadata(uri, intentMimeType)
            } catch (error: Exception) {
                publishIfCurrent(requestId, ViewerUiState(content = ViewerContent.Error("Unable to read file details: ${error.message}")))
                return@launch
            }
            val handler = FileHandlerRegistry(listOf(TurtleFileHandler(reader), GpxFileHandler(reader), JsonFileHandler(reader))).handlerFor(file)
            val format = when (handler) {
                is TurtleFileHandler -> SyntaxFormat.TURTLE
                is GpxFileHandler -> SyntaxFormat.XML
                else -> null
            }
            // Establish the Turtle readable tab before the first composition. The screen uses
            // GPX size to keep dense source documents out of its initial UI composition.
            val initialReadable = if (handler is TurtleFileHandler) ReadableRdfState.Loading else null
            val initialGpx = if (handler is GpxFileHandler) ReadableGpxState.Loading else null
            val initialJson = if (handler is JsonFileHandler) ReadableJsonState.Loading else null
            publishIfCurrent(requestId, ViewerUiState(file = file, loading = true, readableRdf = initialReadable, readableGpx = initialGpx, readableJson = initialJson))
            val content = try {
                handler?.load(file)
                    ?: ViewerContent.Error("This does not appear to be a Turtle (.ttl), GPX (.gpx), or JSON (.json) file.")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.e(LOG_TAG, "Unable to load $uri", error)
                ViewerContent.Error("Unable to open this file. See Logcat for details.")
            }
            if (content !is ViewerContent.Text) {
                publishIfCurrent(requestId, ViewerUiState(file = file, content = content, syntaxFormat = format))
                return@launch
            }

            coroutineScope {
                val gpxParse = if (initialGpx != null) async(Dispatchers.Default) { parseGpx(context, uri) } else null
                val rdfParse = if (initialReadable != null) async(Dispatchers.Default) { parseRdf(context, uri) } else null
                val chunks = if (content.value.length > LazySourceThreshold) {
                    async(Dispatchers.Default) { SourceChunks.from(content.value) }
                } else null
                val jsonFormat = if (initialJson != null) async(Dispatchers.Default) {
                    val formatted = JsonPrettyPrinter.format(content.value)
                    ReadableJsonState.Ready(formatted, SourceChunks.from(formatted))
                } else null

                // A single Text node measures every character on the UI thread. Keep a large
                // source out of composition until its bounded chunks are ready, then let the
                // lazy list compose only the visible chunks.
                publishIfCurrent(requestId, ViewerUiState(file, content, format,
                    sourceLoading = format != null,
                    readableRdf = initialReadable, readableGpx = initialGpx, readableJson = initialJson))
                val highlights = if (chunks == null) format?.let { sourceFormat ->
                    withContext(Dispatchers.Default) {
                        val light = annotatedString(content.value, sourceFormat)
                        light to light.withSyntaxColors(darkSyntaxColors)
                    }
                } else null
                val sourceChunks = chunks?.await()
                val lightHighlighted = highlights?.first
                val darkHighlighted = highlights?.second
                var currentGpx: ReadableGpxState? = initialGpx
                var currentRdf: ReadableRdfState? = initialReadable
                // Raw chunks are usable as soon as they are ready; JSON formatting must never
                // hold the Raw tab behind a second, potentially slower background operation.
                publishIfCurrent(requestId, ViewerUiState(file, content, format,
                    highlightedSource = lightHighlighted, darkHighlightedSource = darkHighlighted,
                    sourceChunks = sourceChunks, readableRdf = currentRdf, readableGpx = currentGpx,
                    readableJson = initialJson))
                val currentJson = jsonFormat?.await()
                publishIfCurrent(requestId, ViewerUiState(file, content, format,
                    highlightedSource = lightHighlighted, darkHighlightedSource = darkHighlighted,
                    sourceChunks = sourceChunks, readableRdf = currentRdf, readableGpx = currentGpx, readableJson = currentJson))

                gpxParse?.let { parse ->
                    currentGpx = parse.await()
                    publishIfCurrent(requestId, ViewerUiState(file, content, format,
                        highlightedSource = lightHighlighted, darkHighlightedSource = darkHighlighted,
                        sourceChunks = sourceChunks, readableRdf = currentRdf, readableGpx = currentGpx, readableJson = currentJson))
                }
                rdfParse?.let { parse ->
                    val readable = parse.await()
                    val document = (readable as? ReadableRdfState.Ready)?.document
                    currentRdf = if (document?.roots?.isEmpty() == true) ReadableRdfState.Empty else readable
                    publishIfCurrent(requestId, ViewerUiState(file, content, format,
                        highlightedSource = lightHighlighted, darkHighlightedSource = darkHighlighted,
                        sourceChunks = sourceChunks, readableRdf = currentRdf, readableGpx = currentGpx, readableJson = currentJson))
                }
            }
        }
    }

    private fun parseGpx(context: Context, uri: Uri): ReadableGpxState {
                val readable = try {
                    context.contentResolver.openInputStream(uri)?.use { GpxReadableParser.parse(it) }
                        ?.let { ReadableGpxState.Ready(gpxDisplayItems(it)) }
                        ?: ReadableGpxState.Error("The selected provider did not provide file contents.")
                } catch (error: CancellationException) { throw error
                } catch (error: Throwable) {
                    Log.w(LOG_TAG, "GPX parse error for $uri", error)
                    ReadableGpxState.Error("Unable to parse GPX: ${error.message?.substringBefore('\n') ?: "invalid XML"}")
                }
        return readable
    }

    private fun parseRdf(context: Context, uri: Uri): ReadableRdfState {
                val readable = try {
                    context.contentResolver.openInputStream(uri)?.use { TurtleRdfParser.parse(it, uri.toString()) }
                        ?.let(ReadableRdfState::Ready) ?: ReadableRdfState.Error("The selected provider did not provide file contents.")
                } catch (error: CancellationException) {
                    throw error
                } catch (error: RDFParseException) {
                    Log.w(LOG_TAG, "Turtle parse error for $uri", error)
                    val location = if (error.lineNumber >= 0) " (line ${error.lineNumber}, column ${error.columnNumber})" else ""
                    ReadableRdfState.Error("Turtle parse error$location: ${error.message?.substringBefore('\n') ?: "Invalid Turtle"}")
                } catch (error: Throwable) {
                    Log.e(LOG_TAG, "Unable to build Turtle outline for $uri", error)
                    ReadableRdfState.Error(
                        message = "Unable to build readable outline.",
                        technicalDetails = RdfErrorDetails.from(error)
                    )
                }
        return readable
    }

    private fun publishIfCurrent(requestId: Long, state: ViewerUiState) {
        if (requestGeneration.get() == requestId) _state.value = state
    }

    private companion object {
        const val LOG_TAG = "TurtleViewer"
        const val LazySourceThreshold = 256 * 1024
    }
}
