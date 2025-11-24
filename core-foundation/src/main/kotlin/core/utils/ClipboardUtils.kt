package core.utils

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.awt.image.ImageObserver
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * System clipboard integration. Extracts images from clipboard for translation workflow.
 */
object ClipboardUtils {
  /**
   * Retrieves image from system clipboard, handles awt/buffered image conversion.
   */
  fun getClipboardImage(): BufferedImage? {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val transferable = clipboard.getContents(null)

    return if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
      try {
        getImage(transferable.getTransferData(DataFlavor.imageFlavor) as Image?)
      } catch (e: Exception) {
        e.printStackTrace()
        null
      }
    } else {
      null
    }
  }

  private fun getImage(image: Image?): BufferedImage? {
    if (image == null) return null
    if (image is BufferedImage) return image
    val lock: Lock = ReentrantLock()
    val size: Condition = lock.newCondition()
    val data: Condition = lock.newCondition()
    val o: ImageObserver = object : ImageObserver {
      override fun imageUpdate(img: Image?, infoflags: Int, x: Int, y: Int, width: Int, height: Int): Boolean {
        lock.lock()
        try {
          if (infoflags and ImageObserver.ALLBITS != 0) {
            size.signal()
            data.signal()
            return false
          }
          if (infoflags and (ImageObserver.WIDTH or ImageObserver.HEIGHT) != 0) size.signal()
          return true
        } finally {
          lock.unlock()
        }
      }
    }
    val bi: BufferedImage
    lock.lock()
    try {
      var width = image.getWidth(o)
      var height = image.getHeight(o)
      while (width < 0 || height < 0) {
        width = image.getWidth(o)
        height = image.getHeight(o)
        size.awaitUninterruptibly()
      }
      bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      val g: Graphics2D = bi.createGraphics()
      try {
        g.background = Color(0, true)
        g.clearRect(0, 0, width, height)
        while (!g.drawImage(image, 0, 0, o)) data.awaitUninterruptibly()
      } finally {
        g.dispose()
      }
    } finally {
      lock.unlock()
    }
    return bi
  }
}