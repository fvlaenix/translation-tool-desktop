package app.utils

import androidx.compose.ui.awt.ComposeWindow
import java.awt.FileDialog
import java.io.File

fun openFileDialog(window: ComposeWindow, title: String, allowMultiSelection: Boolean = true): Set<File> {
  return FileDialog(window, title, FileDialog.LOAD).apply {
    isMultipleMode = allowMultiSelection
    isVisible = true
  }.files.toSet()
}