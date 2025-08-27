package core.image

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.awt.image.BufferedImage

class CoordinateTransformerTest {

  private lateinit var state: ImageCanvasState
  private lateinit var transformer: CoordinateTransformer

  @BeforeEach
  fun setup() {
    state = ImageCanvasState()
    transformer = CoordinateTransformer(state)
  }

  @Test
  fun `test no image returns zero coordinates`() {
    val canvasPoint = transformer.imageToCanvas(Offset(100f, 100f))
    val imagePoint = transformer.canvasToImage(Offset(100f, 100f))

    assertEquals(Offset.Zero, canvasPoint)
    assertEquals(Offset.Zero, imagePoint)
  }

  @Test
  fun `test round trip coordinate transformation maintains accuracy`() {
    val testImage = BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(400, 400))

    val originalImagePoint = Offset(50f, 75f)
    val canvasPoint = transformer.imageToCanvas(originalImagePoint)
    val backToImagePoint = transformer.canvasToImage(canvasPoint)

    val tolerance = 0.001f
    assertEquals(originalImagePoint.x, backToImagePoint.x, tolerance)
    assertEquals(originalImagePoint.y, backToImagePoint.y, tolerance)
  }

  @Test
  fun `test image centered in larger canvas`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(300, 300))

    val imageCenterPoint = Offset(50f, 50f)
    val canvasPoint = transformer.imageToCanvas(imageCenterPoint)

    assertEquals(150f, canvasPoint.x, 0.1f)
    assertEquals(150f, canvasPoint.y, 0.1f)
  }

  @Test
  fun `test image scales correctly when canvas is smaller`() {
    val testImage = BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(200, 200))

    val scale = state.imageToCanvasScale

    assertEquals(0.5f, scale, 0.001f)

    val imageCorner = Offset(400f, 400f)
    val canvasPoint = transformer.imageToCanvas(imageCorner)
    assertEquals(200f, canvasPoint.x, 0.1f)
    assertEquals(200f, canvasPoint.y, 0.1f)
  }

  @ParameterizedTest
  @CsvSource(
    "100, 100, 200, 200, 2.0",
    "200, 200, 100, 100, 0.5",
    "100, 100, 100, 100, 1.0",
    "200, 100, 400, 400, 2.0",
    "100, 200, 400, 400, 2.0"
  )
  fun `test scaling calculation with different image and canvas sizes`(
    imageWidth: Int, imageHeight: Int,
    canvasWidth: Int, canvasHeight: Int,
    expectedScale: Float
  ) {
    val testImage = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(canvasWidth, canvasHeight))

    val actualScale = state.imageToCanvasScale

    assertEquals(expectedScale, actualScale, 0.001f)
  }

  @Test
  fun `test rectangle transformation preserves aspect ratio`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(200, 200))

    val imageRect = Rect(offset = Offset(10f, 20f), size = Size(30f, 40f))
    val canvasRect = transformer.imageRectToCanvas(imageRect)

    assertEquals(20f, canvasRect.left, 0.1f)
    assertEquals(40f, canvasRect.top, 0.1f)
    assertEquals(60f, canvasRect.width, 0.1f)
    assertEquals(80f, canvasRect.height, 0.1f)
  }

  @Test
  fun `test bounds checking for image coordinates`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(200, 200))

    assertTrue(transformer.isPointInImageBounds(Offset(50f, 50f)))
    assertTrue(transformer.isPointInImageBounds(Offset(0f, 0f)))
    assertTrue(transformer.isPointInImageBounds(Offset(100f, 100f)))

    assertFalse(transformer.isPointInImageBounds(Offset(-1f, 50f)))
    assertFalse(transformer.isPointInImageBounds(Offset(50f, -1f)))
    assertFalse(transformer.isPointInImageBounds(Offset(101f, 50f)))
    assertFalse(transformer.isPointInImageBounds(Offset(50f, 101f)))
  }

  @Test
  fun `test clamping points to image bounds`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(200, 200))

    val clampedPoint1 = transformer.clampPointToImageBounds(Offset(-10f, -10f))
    val clampedPoint2 = transformer.clampPointToImageBounds(Offset(110f, 110f))
    val clampedPoint3 = transformer.clampPointToImageBounds(Offset(50f, 50f))

    assertEquals(Offset(0f, 0f), clampedPoint1)
    assertEquals(Offset(100f, 100f), clampedPoint2)
    assertEquals(Offset(50f, 50f), clampedPoint3)
  }

  @Test
  fun `test distance scaling`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(200, 200))

    val imageDistance = 25f
    val canvasDistance = transformer.imageDistanceToCanvas(imageDistance)
    val backToImageDistance = transformer.canvasDistanceToImage(canvasDistance)

    assertEquals(50f, canvasDistance, 0.1f)
    assertEquals(25f, backToImageDistance, 0.1f)
  }

  @Test
  fun `test aspect ratio preservation with non-square canvas`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(400, 200))

    val scale = state.imageToCanvasScale
    val displaySize = state.imageDisplaySize
    val offset = state.imageOffsetInCanvas

    assertEquals(2.0f, scale, 0.001f)
    assertEquals(200, displaySize.width)
    assertEquals(200, displaySize.height)
    assertEquals(100f, offset.x, 0.1f)
    assertEquals(0f, offset.y, 0.1f)

    val imagePoint = Offset(10f, 10f)
    val canvasPoint = transformer.imageToCanvas(imagePoint)
    assertEquals(120f, canvasPoint.x, 0.1f)
    assertEquals(20f, canvasPoint.y, 0.1f)
  }

  @Test
  fun `test zero or negative image dimensions handled gracefully`() {
    val testImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)

    state.setImage(testImage)
    state.updateCanvasSize(IntSize(0, 0))

    val canvasPoint = transformer.imageToCanvas(Offset(10f, 10f))
    val imagePoint = transformer.canvasToImage(Offset(10f, 10f))

    assertNotNull(canvasPoint)
    assertNotNull(imagePoint)
  }

  @Test
  fun `test state validation`() {
    val testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    state.setImage(testImage)
    state.updateCanvasSize(IntSize(200, 200))

    val isValid = state.debugValidateState()

    assertTrue(isValid)
  }
}