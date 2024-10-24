package app.batch

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.AppStateEnum
import app.TopBar
import app.project.images.ImageProjectPanelState
import app.utils.openFileDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import project.Project
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable
fun ImageDataCreator(
  state: MutableState<AppStateEnum>,
  projectState: MutableState<ImageProjectPanelState>? = null,
  project: Project? = null
) {
  val parent = remember { ComposeWindow(null) }
  var isLoading by remember { mutableStateOf(false) }
  var progress by remember { mutableStateOf(0f) }

  val parentSize = remember { mutableStateOf(IntSize.Zero) }
  val imagesService: ImagesService = if (project == null) {
    BatchService.getInstance()
  } else {
    when (projectState!!.value) {
      ImageProjectPanelState.UNTRANSLATED_IMAGES_CREATOR -> ImageDataService.getInstance(project, ImageDataService.UNTRANSLATED)
      ImageProjectPanelState.CLEANED_IMAGES_CREATOR -> ImageDataService.getInstance(project, ImageDataService.CLEANED)
      else -> throw IllegalStateException()
    }
  }
  val scope = rememberCoroutineScope()

  TopBar(state, "Batch Creator") {
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
              imagesService.clear()
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

        val images = imagesService.get()

        for (image in images) {
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
}