package io.github.zeroone3010.turtleviewer.model

sealed interface ViewerContent {
    data class Text(val value: String) : ViewerContent
    data class Error(val message: String) : ViewerContent
}
