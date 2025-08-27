package core.image

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class ImageCanvasStateTest {

  private lateinit var state: ImageCanvasState

  @BeforeEach
  fun setup() {
    state = ImageCanvasState()
  }

  @Test
  fun `test initial state`() {
    assertNull(state.image)
    assertEquals(IntSize.Zero, state.canvasSize)
    assertEquals(IntSize.Zero, state.imageDisplaySize)
    assertEquals(1f, state.imageToCanvasScale)
    assertEquals(Offset.Zero, state.imageOffsetInCanvas)
    assertFalse(state.isLoading)
    assertFalse(state.hasImage)
    assertEquals(IntSize.Zero, state.imageSize)
  }

  @Test
  fun `test image setting updates derived properties`() {
    val testImage = BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB)

    state.setImage(testImage)

    assertEquals(testImage, state.image)
    assertTrue(state.hasImage)
    assertEquals(IntSize(100, 50), state.imageSize)
    assertFalse(state.isLoading)
  }

  @Test
  fun `test canvas size update triggers recalculation`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(200, 200))

    val initialScale = state.imageToCanvasScale
    state.updateCanvasSize(IntSize(400, 400))

    assertNotEquals(initialScale, state.imageToCanvasScale)
    assertEquals(4f, state.imageToCanvasScale, 0.001f)
  }

  @Test
  fun `test loading state management`() {
    assertFalse(state.isLoading)

    state.setLoading(true)

    assertTrue(state.isLoading)

    state.setLoading(false)

    assertFalse(state.isLoading)
  }

  @Test
  fun `test image replacement handles loading state`() {
    val image1 = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(image1)
    assertFalse(state.isLoading)

    val image2 = BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB)
    state.setImage(image2)

    assertFalse(state.isLoading)
    assertEquals(image2, state.image)
  }

  @Test
  fun `test removing image resets state`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(400, 300))

    assertTrue(state.hasImage)
    assertNotEquals(1f, state.imageToCanvasScale)
    assertNotEquals(Offset.Zero, state.imageOffsetInCanvas)

    state.setImage(null)

    assertFalse(state.hasImage)
    assertNull(state.image)
    assertEquals(IntSize.Zero, state.imageSize)
    assertEquals(IntSize.Zero, state.imageDisplaySize)
    assertEquals(1f, state.imageToCanvasScale)
    assertEquals(Offset.Zero, state.imageOffsetInCanvas)
  }

  @Test
  fun `test image bounds calculation`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(300, 300))

    val bounds = state.getImageBoundsInCanvas()

    assertEquals(0f, bounds.left, 0.1f)
    assertEquals(0f, bounds.top, 0.1f)
    assertEquals(300f, bounds.width, 0.1f)
    assertEquals(300f, bounds.height, 0.1f)
  }

  @Test
  fun `test image bounds with non-square aspect ratios`() {
    val testImage = BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(400, 400))

    val bounds = state.getImageBoundsInCanvas()

    assertEquals(2f, state.imageToCanvasScale, 0.001f)
    assertEquals(IntSize(400, 200), state.imageDisplaySize)
    assertEquals(0f, state.imageOffsetInCanvas.x, 0.1f)
    assertEquals(100f, state.imageOffsetInCanvas.y, 0.1f)
  }

  @Test
  fun `test zero canvas size handling`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(0, 0))

    assertEquals(1f, state.imageToCanvasScale)
    assertEquals(IntSize.Zero, state.imageDisplaySize)
    assertEquals(Offset.Zero, state.imageOffsetInCanvas)
  }

  @Test
  fun `test same image setting does not trigger recalculation`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(200, 200))
    val initialScale = state.imageToCanvasScale

    state.setImage(testImage)

    assertEquals(initialScale, state.imageToCanvasScale)
  }

  @Test
  fun `test same canvas size does not trigger recalculation`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(200, 200))
    val initialScale = state.imageToCanvasScale

    state.updateCanvasSize(IntSize(200, 200))

    assertEquals(initialScale, state.imageToCanvasScale)
  }

  @Test
  fun `test debug validation with valid state`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(200, 200))

    val isValid = state.debugValidateState()

    assertTrue(isValid)
  }

  @Test
  fun `test debug validation with no image`() {
    state.updateCanvasSize(IntSize(200, 200))

    val isValid = state.debugValidateState()

    assertTrue(isValid)
  }
}