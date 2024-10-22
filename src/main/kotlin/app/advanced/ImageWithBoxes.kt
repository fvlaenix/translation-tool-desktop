package app.advanced

import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Magenta
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import bean.BlockPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import utils.ClipboardUtils.getClipboardImage
import utils.FollowableMutableState
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable
fun ImageWithBoxes(
  image: MutableState<ImageBitmap?>,
  boxes: SnapshotStateList<BlockPosition>,
  isEnabled: MutableState<Boolean>,
  currentSize: FollowableMutableState<IntSize>
) {
  val requester = remember { FocusRequester() }
  val emptyText = remember { mutableStateOf("Press CTRL+V to insert image\nThen press CTRL+N to create box to translate,\nDelete to delete previous box") }

  isEnabled.value = image.value != null

  val scope = rememberCoroutineScope()

  Row(
    modifier = Modifier
      .fillMaxSize()
      .onKeyEvent { keyEvent ->
        if (keyEvent.type.toString() != "KeyUp") return@onKeyEvent true
        if (keyEvent.isCtrlPressed && keyEvent.key == Key.V) {
          emptyText.value = "Image is loading"
          scope.launch(Dispatchers.IO) {
            val clipboardImage = getClipboardImage()
            if (clipboardImage == null) {
              emptyText.value = "Can't take image from clipboard"
            } else {
              val outputStream = ByteArrayOutputStream()
              ImageIO.write(clipboardImage, "png", outputStream)
              val byteArray = outputStream.toByteArray()
              image.value = loadImageBitmap(ByteArrayInputStream(byteArray))
            }
          }
        }
        if (keyEvent.isCtrlPressed && keyEvent.key == Key.N) {
          if (image.value != null) {
            boxes += BlockPosition(
              x = .0,
              y = .0,
              width = image.value!!.width.toDouble() / 10,
              height = image.value!!.height.toDouble() / 10,
              shape = BlockPosition.Shape.Rectangle
            )
          }
        }
        if (keyEvent.key == Key.Delete) {
          if (boxes.isNotEmpty()) boxes.dropLast(1)
        }
        false
      }
      .focusRequester(requester)
      .focusable()
  ) {
    val imageNotNull = image.value
    if (imageNotNull != null) {
      val imageOriginalSize = IntSize(imageNotNull.width, imageNotNull.height)

      Box(modifier = Modifier.fillMaxSize()) {
        Image(
          bitmap = imageNotNull,
          contentDescription = null,
          modifier = Modifier.fillMaxSize()
            .onSizeChanged { size -> currentSize.value = size },
          alignment = Alignment.TopStart
        )
        boxes.forEachIndexed { index, box ->
          val boxFollowable = remember { FollowableMutableState(mutableStateOf(box)) }
          boxFollowable.follow { _, after ->
            boxes[index] = after
          }
          BoxOnImage(imageOriginalSize, currentSize.value, boxFollowable)
        }
      }
    } else {
      val gradientColors = listOf(Cyan, Gray, Magenta)
      Text(
        text = emptyText.value,
        modifier = Modifier.fillMaxSize(),
        textAlign = TextAlign.Center,
        fontSize = 20.sp,
        style = TextStyle(
          brush = Brush.linearGradient(
            colors = gradientColors
          )
        )
      )
    }
  }
  LaunchedEffect(Unit) {
    requester.requestFocus()
  }
}