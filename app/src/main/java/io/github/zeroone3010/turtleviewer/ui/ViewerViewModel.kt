package io.github.zeroone3010.turtleviewer.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zeroone3010.turtleviewer.files.FileHandlerRegistry
import io.github.zeroone3010.turtleviewer.files.TurtleFileHandler
import io.github.zeroone3010.turtleviewer.files.UriFileReader
import io.github.zeroone3010.turtleviewer.model.OpenedFile
import io.github.zeroone3010.turtleviewer.model.ViewerContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ViewerUiState(val file: OpenedFile? = null, val content: ViewerContent? = null, val loading: Boolean = false)

class ViewerViewModel : ViewModel() {
    private val _state = MutableStateFlow(ViewerUiState())
    val state: StateFlow<ViewerUiState> = _state.asStateFlow()

    fun open(context: Context, uri: Uri, intentMimeType: String? = null) = viewModelScope.launch(Dispatchers.IO) {
        val reader = UriFileReader(context.contentResolver)
        val file = try { reader.metadata(uri, intentMimeType) } catch (error: Exception) {
            _state.value = ViewerUiState(content = ViewerContent.Error("Unable to read file details: ${error.message}")); return@launch
        }
        _state.value = ViewerUiState(file = file, loading = true)
        val handler = FileHandlerRegistry(listOf(TurtleFileHandler(reader))).handlerFor(file)
        _state.value = ViewerUiState(file = file, content = handler?.load(file)
            ?: ViewerContent.Error("This does not appear to be a Turtle (.ttl) file."))
    }
}
