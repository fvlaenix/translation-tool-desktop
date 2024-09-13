package utils

import bean.BlockSettings
import java.awt.BasicStroke
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.GlyphVector
import java.awt.image.BufferedImage
import kotlin.math.min

object Text2ImageUtils {
  private fun splitToLines(text: String, textMetrics: FontMetrics, width: Int): List<String> {
    val lines = text.lines()
    return lines.flatMap { line ->
      val arr = line.split(" ".toRegex()).toTypedArray()
      var nIndex = 0
      val result = mutableListOf<String>()
      while (nIndex < arr.size) {
        var line = arr[nIndex++]
        while ((nIndex < arr.size) && (textMetrics.stringWidth(line + " " + arr[nIndex]) < width)) {
          line = line + " " + arr[nIndex]
          nIndex++
        }
        result.add(line)
      }
      result
    }
  }

  private fun textToImage(
    graphics2D: Graphics2D,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    text: String,
    settings: BlockSettings
  ): Boolean {
    graphics2D.font = settings.font
    val fontMetrics = graphics2D.fontMetrics
    val textX = x + settings.border
    val textWidth = width - settings.border * 2
    val outlineStroke = BasicStroke(settings.outlineSize.toFloat())

    val lines = splitToLines(text, fontMetrics, textWidth)
    val isOutOfBorder = lines.size * fontMetrics.height + settings.border > height

    // background
    val backgroundColor = settings.backgroundColor
    if (backgroundColor.a != 0) {
      graphics2D.color = backgroundColor.color
      val height = min(lines.size * fontMetrics.height + settings.border * 2, height)
      graphics2D.fillRect(x, y, width, height)
    }

    // text
    val lineHeight = fontMetrics.height
    var startY = y + settings.fontSize + settings.border
    lines.forEach { line ->
      val originalStroke = graphics2D.stroke
      val originalHints = graphics2D.renderingHints

      // create a glyph vector from text
      val glyphVector: GlyphVector = graphics2D.font.createGlyphVector(graphics2D.fontRenderContext, line)
      graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

      val currentTextWidth = fontMetrics.stringWidth(line)
      val startX = textX + (textWidth - currentTextWidth) / 2

      // get the shape object
      val textShape = glyphVector.getOutline(startX.toFloat(), startY.toFloat())

      graphics2D.color = settings.outlineColor.color
      graphics2D.stroke = outlineStroke
      graphics2D.draw(textShape)

      graphics2D.color = settings.fontColor.color
      graphics2D.drawString(line, startX, startY)
      startY += lineHeight

      graphics2D.stroke = originalStroke
      graphics2D.setRenderingHints(originalHints)
    }
    return isOutOfBorder
  }

  fun createSample(
    width: Int,
    height: Int,
    settings: BlockSettings
  ): BufferedImage {
    val text =
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer rhoncus iaculis orci, a interdum risus pellentesque et. Pellentesque fringilla massa"
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics2D = image.createGraphics()
    textToImage(graphics2D, 0, 0, width, height, text, settings)
    return image
  }
}