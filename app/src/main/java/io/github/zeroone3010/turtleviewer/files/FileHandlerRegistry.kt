package io.github.zeroone3010.turtleviewer.files

import io.github.zeroone3010.turtleviewer.model.OpenedFile

class FileHandlerRegistry(private val handlers: List<FileHandler>) {
    fun handlerFor(file: OpenedFile): FileHandler? = handlers.firstOrNull { it.canHandle(file) }
}
