package io.github.zeroone3010.turtleviewer.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zeroone3010.turtleviewer.files.FileHandlerRegistry
import io.github.zeroone3010.turtleviewer.files.GpxFileHandler
import io.github.zeroone3010.turtleviewer.files.TurtleFileHandler
import io.github.zeroone3010.turtleviewer.files.UriFileReader
import io.github.zeroone3010.turtleviewer.model.OpenedFile
import io.github.zeroone3010.turtleviewer.model.ViewerContent
import io.github.zeroone3010.turtleviewer.rdf.ReadableRdfState
import io.github.zeroone3010.turtleviewer.rdf.TurtleRdfParser
import org.eclipse.rdf4j.rio.RDFParseException
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ViewerUiState(
    val file: OpenedFile? = null,
    val content: ViewerContent? = null,
    val syntaxFormat: SyntaxFormat? = null,
    val loading: Boolean = false,
    val readableRdf: ReadableRdfState? = null
)

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
            publishIfCurrent(requestId, ViewerUiState(file = file, loading = true))
            val handler = FileHandlerRegistry(listOf(TurtleFileHandler(reader), GpxFileHandler(reader))).handlerFor(file)
            val content = handler?.load(file)
                ?: ViewerContent.Error("This does not appear to be a Turtle (.ttl) or GPX (.gpx) file.")
            val format = when (handler) {
                is TurtleFileHandler -> SyntaxFormat.TURTLE
                is GpxFileHandler -> SyntaxFormat.XML
                else -> null
            }
            val initialReadable = if (handler is TurtleFileHandler && content is ViewerContent.Text) ReadableRdfState.Loading else null
            publishIfCurrent(requestId, ViewerUiState(file = file, content = content, syntaxFormat = format, readableRdf = initialReadable))
            if (initialReadable != null) {
                val readable = try {
                    context.contentResolver.openInputStream(uri)?.use { TurtleRdfParser.parse(it, uri.toString()) }
                        ?.let(ReadableRdfState::Ready) ?: ReadableRdfState.Error("The selected provider did not provide file contents.")
                } catch (error: RDFParseException) {
                    val location = if (error.lineNo >= 0) " (line ${error.lineNo}, column ${error.columnNo})" else ""
                    ReadableRdfState.Error("Turtle parse error$location: ${error.message?.substringBefore('\n') ?: "Invalid Turtle"}")
                } catch (error: Exception) { ReadableRdfState.Error("Unable to build readable outline: ${error.message ?: "Unknown error"}") }
                val document = (readable as? ReadableRdfState.Ready)?.document
                publishIfCurrent(requestId, ViewerUiState(file, content, format, readableRdf = if (document?.roots?.isEmpty() == true) ReadableRdfState.Empty else readable))
            }
        }
    }

    private fun publishIfCurrent(requestId: Long, state: ViewerUiState) {
        if (requestGeneration.get() == requestId) _state.value = state
    }
}
