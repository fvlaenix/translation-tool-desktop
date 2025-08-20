package app.batch

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.TopBar
import app.project.images.ImageProjectPanelState
import app.utils.openFileDialog
import core.navigation.NavigationController
import core.utils.ClipboardUtils.getClipboardImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import project.data.ImageDataRepository
import project.data.ImageType
import project.data.Project
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable
fun ImageDataCreator(
  navigationController: NavigationController,
  projectState: MutableState<ImageProjectPanelState>? = null,
  project: Project? = null
) {
  val parent = remember { ComposeWindow(null) }
  var isLoading by remember { mutableStateOf(false) }
  var progress by remember { mutableStateOf(0f) }

  val parentSize = remember { mutableStateOf(IntSize.Zero) }
  val imageDataRepository: ImageDataRepository = koinInject()
  val scope = rememberCoroutineScope()
  val requester = remember { FocusRequester() }

  // Store current images list for display
  var currentImages by remember { mutableStateOf<List<ImagePathInfo>>(emptyList()) }

  // Create images service wrapper for project vs batch logic
  val imagesService: ImagesService = remember(project, projectState?.value) {
    if (project == null) {
      object : ImagesService {
        override suspend fun add(image: ImagePathInfo) {
          imageDataRepository.addToBatch(image).getOrThrow()
          refreshImages()
        }

        override suspend fun clear() {
          imageDataRepository.clearBatch().getOrThrow()
          refreshImages()
        }

        override suspend fun get(): List<ImagePathInfo> {
          return imageDataRepository.getBatchImages().getOrElse { emptyList() }
        }

        override suspend fun saveIfRequired() {
          // Batch operations are in-memory, no need to save
          // TODO: Consider making batch operations persistent
        }

        private suspend fun refreshImages() {
          currentImages = get()
        }
      }
    } else {
      val imageType = when (projectState!!.value) {
        ImageProjectPanelState.UNTRANSLATED_IMAGES_CREATOR -> ImageType.UNTRANSLATED
        ImageProjectPanelState.CLEANED_IMAGES_CREATOR -> ImageType.CLEANED
        else -> throw IllegalStateException("Unknown project state: ${projectState.value}")
      }

      object : ImagesService {
        override suspend fun add(image: ImagePathInfo) {
          imageDataRepository.addImage(project, imageType, image).getOrThrow()
          refreshImages()
        }

        override suspend fun clear() {
          imageDataRepository.clearImages(project, imageType).getOrThrow()
          refreshImages()
        }

        override suspend fun get(): List<ImagePathInfo> {
          return imageDataRepository.loadImages(project, imageType).getOrElse { emptyList() }
        }

        override suspend fun saveIfRequired() {
          // Images are automatically saved in repository methods
          // TODO: Consider if we need explicit save calls
        }

        private suspend fun refreshImages() {
          currentImages = get()
        }
      }
    }
  }

  // Load initial images
  LaunchedEffect(imagesService) {
    currentImages = imagesService.get()
  }

  TopBar(navigationController, "Batch Creator") {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .onSizeChanged { size -> parentSize.value = size }
    ) {
      Column(
        modifier = Modifier.padding(16.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(requester)
            .focusable()
            .onKeyEvent { keyEvent ->
              if (keyEvent.type.toString() != "KeyUp") return@onKeyEvent true
              if (keyEvent.isCtrlPressed && keyEvent.key == Key.V) {
                scope.launch(Dispatchers.IO) {
                  isLoading = true
                  progress = 0f
                  val clipboardImage = getClipboardImage()
                  if (clipboardImage == null) {
                    // TODO make progress somehow
                  } else {
                    progress = 0.5f
                    imagesService.add(ImagePathInfo(clipboardImage, "clipboard-image"))
                  }
                  imagesService.saveIfRequired()
                  isLoading = false
                }
              }
              false
            }
        ) {
          Button(
            onClick = {
              val files = openFileDialog(parent, "Files to add")
              scope.launch(Dispatchers.IO) {
                isLoading = true
                progress = 0f
                files.forEachIndexed { index, file ->
                  progress = index.toFloat() / files.size
                  val image = ImageIO.read(file)
                  imagesService.add(ImagePathInfo(image, file.nameWithoutExtension))
                }
                imagesService.saveIfRequired()
                isLoading = false
              }
            },
            enabled = !isLoading
          ) {
            Text("Add Files")
          }
          Button(
            onClick = {
              scope.launch {
                imagesService.clear()
              }
            }
          ) {
            Text("Delete Files")
          }
        }

        if (isLoading) {
          LinearProgressIndicator(
            progress,
            modifier = Modifier.fillMaxWidth()
          )
        }

        currentImages.forEach { image ->
          Row(
            modifier = Modifier
              .border(1.dp, Color.Black, CutCornerShape(16.dp))
              .height(parentSize.value.height.dp / 4)
              .fillMaxWidth()
          ) {
            val outputStream = ByteArrayOutputStream()
            ImageIO.write(image.image, "png", outputStream)
            val byteArray = outputStream.toByteArray()
            val bitmapImage = loadImageBitmap(ByteArrayInputStream(byteArray))
            Image(
              bitmap = bitmapImage,
              contentDescription = null,
              modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
                .height(parentSize.value.height.dp / 5)
            )
          }
        }
      }
    }
  }

  LaunchedEffect(Unit) {
    requester.requestFocus()
  }
}