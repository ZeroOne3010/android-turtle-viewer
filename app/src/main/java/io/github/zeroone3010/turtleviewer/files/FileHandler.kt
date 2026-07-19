package io.github.zeroone3010.turtleviewer.files

import io.github.zeroone3010.turtleviewer.model.OpenedFile
import io.github.zeroone3010.turtleviewer.model.ViewerContent

interface FileHandler {
    fun canHandle(file: OpenedFile): Boolean
    suspend fun load(file: OpenedFile): ViewerContent
}
