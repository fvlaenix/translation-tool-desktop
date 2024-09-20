package app.utils

import androidx.compose.ui.awt.ComposeWindow
import java.awt.FileDialog
import java.io.File

fun openFileDialog(
  window: ComposeWindow,
  title: String,
  allowMultiSelection: Boolean = true,
  mode: Int = FileDialog.LOAD
): Set<File> {
  return FileDialog(window, title, mode).apply {
    isMultipleMode = allowMultiSelection
    isVisible = true
  }.files.toSet()
}