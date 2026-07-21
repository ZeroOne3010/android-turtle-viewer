package io.github.zeroone3010.turtleviewer.ui

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zeroone3010.turtleviewer.files.FileHandlerRegistry
import io.github.zeroone3010.turtleviewer.files.TurtleFileHandler
import io.github.zeroone3010.turtleviewer.files.UriFileReader
import io.github.zeroone3010.turtleviewer.model.OpenedFile
import io.github.zeroone3010.turtleviewer.model.ViewerContent
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
    val highlightedText: AnnotatedString? = null,
    val loading: Boolean = false
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
            val handler = FileHandlerRegistry(listOf(TurtleFileHandler(reader))).handlerFor(file)
            val content = handler?.load(file)
                ?: ViewerContent.Error("This does not appear to be a Turtle (.ttl) file.")
            val highlightedText = (content as? ViewerContent.Text)?.value?.let(::turtleAnnotatedString)
            publishIfCurrent(requestId, ViewerUiState(file = file, content = content, highlightedText = highlightedText))
        }
    }

    private fun publishIfCurrent(requestId: Long, state: ViewerUiState) {
        if (requestGeneration.get() == requestId) _state.value = state
    }
}
