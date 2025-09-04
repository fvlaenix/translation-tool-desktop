package translation.domain

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import app.batch.ImagePathInfo
import core.base.BaseViewModel
import core.utils.ImageUtils.deepCopy
import core.utils.Text2ImageUtils
import fonts.domain.FontResolver
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import project.data.ImageDataRepository
import project.data.Project
import project.data.TextDataRepository
import project.data.TextType
import translation.data.BlockSettings
import translation.data.ImageData
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory

/**
 * Data class combining image path info with image data for editing.
 */
data class EditImageData(
  val imagePathInfo: ImagePathInfo,
  val imageData: ImageData
)

/**
 * View model for editing image data with text blocks and generating final images.
 */
class EditCreatorViewModel(
  private val imageDataRepository: ImageDataRepository,
  private val textDataRepository: TextDataRepository,
  private val fontResolver: FontResolver
) : BaseViewModel() {

  private val _currentEditData = mutableStateOf<EditImageData?>(null)
  val currentEditData: State<EditImageData?> = _currentEditData

  private val _selectedBoxIndex = mutableStateOf<Int?>(null)
  val selectedBoxIndex: State<Int?> = _selectedBoxIndex

  private val _currentSettings = mutableStateOf<BlockSettings?>(null)
  val currentSettings: State<BlockSettings?> = _currentSettings

  private val _isGenerating = mutableStateOf(false)
  val isGenerating: State<Boolean> = _isGenerating

  private val _generationProgress = mutableStateOf(0f)
  val generationProgress: State<Float> = _generationProgress

  fun loadEditData(imagePathInfo: ImagePathInfo, imageData: ImageData) {
    _currentEditData.value = EditImageData(imagePathInfo, imageData)
    _selectedBoxIndex.value = null
    updateCurrentSettings()
    clearError()
  }

  fun selectBox(index: Int?) {
    _selectedBoxIndex.value = index
    updateCurrentSettings()
  }

  fun updateText(boxIndex: Int, text: String) {
    val currentData = _currentEditData.value ?: return
    val updatedBlockData = currentData.imageData.blockData.toMutableList()

    if (boxIndex in updatedBlockData.indices) {
      updatedBlockData[boxIndex] = updatedBlockData[boxIndex].copy(text = text)

      val updatedImageData = currentData.imageData.copy(blockData = updatedBlockData)
      _currentEditData.value = currentData.copy(imageData = updatedImageData)
    }
  }

  fun updateSettings(settings: BlockSettings) {
    val currentData = _currentEditData.value ?: return
    val selectedIndex = _selectedBoxIndex.value

    if (selectedIndex == null) {
      // Update global settings
      val updatedImageData = currentData.imageData.copy(settings = settings)
      _currentEditData.value = currentData.copy(imageData = updatedImageData)
    } else {
      // Update box-specific settings
      val updatedBlockData = currentData.imageData.blockData.toMutableList()
      if (selectedIndex in updatedBlockData.indices) {
        updatedBlockData[selectedIndex] = updatedBlockData[selectedIndex].copy(settings = settings)

        val updatedImageData = currentData.imageData.copy(blockData = updatedBlockData)
        _currentEditData.value = currentData.copy(imageData = updatedImageData)
      }
    }

    _currentSettings.value = settings
  }

  fun updateBoxShape(boxIndex: Int, shape: translation.data.BlockPosition.Shape) {
    val currentData = _currentEditData.value ?: return
    val updatedBlockData = currentData.imageData.blockData.toMutableList()

    if (boxIndex in updatedBlockData.indices) {
      val block = updatedBlockData[boxIndex]
      val updatedPosition = block.blockPosition.copy(shape = shape)
      updatedBlockData[boxIndex] = block.copy(blockPosition = updatedPosition)

      val updatedImageData = currentData.imageData.copy(blockData = updatedBlockData)
      _currentEditData.value = currentData.copy(imageData = updatedImageData)
    }
  }

  fun generateFinalImages(editDataList: List<EditImageData>, outputPath: String, project: Project?) {
    viewModelScope.launch {
      _isGenerating.value = true
      _generationProgress.value = 0f
      clearError()

      try {
        if (project != null) {
          // Save updated translation data to project
          val translatedDataService = textDataRepository
          val currentWorkData = textDataRepository.loadWorkData(project, TextType.TRANSLATED).getOrNull()

          if (currentWorkData != null) {
            val updatedWorkData = currentWorkData.copy(
              imagesData = editDataList.map { it.imageData }
            )
            textDataRepository.saveWorkData(project, TextType.TRANSLATED, updatedWorkData).getOrThrow()
          }
        }

        // Generate final images
        val outputPathObj = Path.of(outputPath)
        outputPathObj.createDirectories()

        if (!outputPathObj.isDirectory()) {
          throw IllegalArgumentException("Output path is not a directory")
        }

        val progressIncrement = 1.0f / editDataList.size
        val semaphore = Semaphore(4) // Limit concurrent processing

        withContext(Dispatchers.IO) {
          editDataList.mapIndexed { index, editData ->
            async {
              semaphore.withPermit {
                processImageWithBlocks(editData, index, editDataList.size, outputPathObj)
                _generationProgress.value += progressIncrement
                _generationProgress.value = _generationProgress.value.coerceIn(0f, 1f)
              }
            }
          }.awaitAll()
        }

        _generationProgress.value = 1f

      } catch (e: Exception) {
        setError("Failed to generate final images: ${e.message}")
      }

      _isGenerating.value = false
    }
  }

  private suspend fun processImageWithBlocks(
    editData: EditImageData,
    index: Int,
    totalCount: Int,
    outputPath: Path
  ) {
    val baseImage = editData.imagePathInfo.image.deepCopy()

    // Resolve fonts for all settings
    val resolvedImageData = resolveImageDataFonts(editData.imageData)

    // Generate text images for each block
    val blockImages = resolvedImageData.blockData.map { blockData ->
      val settings = blockData.settings ?: resolvedImageData.settings
      blockData to Text2ImageUtils.textToImage(
        settings,
        blockData.copy(
          blockPosition = blockData.blockPosition.copy(x = 0.0, y = 0.0)
        )
      )
    }

    // Composite the text images onto the base image
    val graphics = baseImage.createGraphics()
    try {
      blockImages.forEach { (blockData, textImageResult) ->
        graphics.drawImage(
          textImageResult.image,
          blockData.blockPosition.x.toInt(),
          blockData.blockPosition.y.toInt(),
          null
        )
      }
    } finally {
      graphics.dispose()
    }

    // Save the final image
    val fileName = "${(index + 1).toString().padStart(totalCount.toString().length + 1, '0')}.png"
    val imagePath = outputPath.resolve(fileName)
    ImageIO.write(baseImage, "PNG", imagePath.toFile())
  }

  private suspend fun resolveImageDataFonts(imageData: ImageData): ImageData {
    val resolvedGlobalSettings = fontResolver.resolveFont(imageData.settings)

    val resolvedBlockData = imageData.blockData.map { blockData ->
      if (blockData.settings != null) {
        val resolvedSettings = fontResolver.resolveFont(blockData.settings)
        blockData.copy(settings = resolvedSettings)
      } else {
        blockData
      }
    }

    return imageData.copy(
      settings = resolvedGlobalSettings,
      blockData = resolvedBlockData
    )
  }

  private fun updateCurrentSettings() {
    val currentData = _currentEditData.value ?: return
    val selectedIndex = _selectedBoxIndex.value

    _currentSettings.value = if (selectedIndex == null) {
      currentData.imageData.settings
    } else {
      val boxIndex = selectedIndex
      if (boxIndex < currentData.imageData.blockData.size) {
        currentData.imageData.blockData[boxIndex].settings ?: currentData.imageData.settings
      } else {
        _selectedBoxIndex.value = null
        currentData.imageData.settings
      }
    }
  }
}