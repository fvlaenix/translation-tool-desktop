package app.advanced.domain

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import app.advanced.TranslationInfo
import translation.data.BlockPosition

/**
 * Ii state for image with boxes functionality, holds image, boxes, selection and display settings.
 */
@Immutable
data class ImageWithBoxesUiState(
  val image: ImageBitmap? = null,
  val boxes: List<BlockPosition> = emptyList(),
  val selectedBoxIndex: Int? = null,
  val isEnabled: Boolean = false,
  val emptyText: String = "Press CTRL+V to insert image\nThen press CTRL+N to create box to translate,\nDelete to delete previous box",
  val currentSize: IntSize = IntSize.Zero,
  val isPreparingTranslation: Boolean = false,
  val preparedTranslationInfos: List<TranslationInfo>? = null
)