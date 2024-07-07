package utils

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage

object ClipboardUtils {
  fun getClipboardImage(): BufferedImage? {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val transferable = clipboard.getContents(null)

    return if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
      try {
        transferable.getTransferData(DataFlavor.imageFlavor) as BufferedImage?
      } catch (e: Exception) {
        e.printStackTrace()
        null
      }
    } else {
      null
    }
  }
}