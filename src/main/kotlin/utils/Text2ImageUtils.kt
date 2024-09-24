package utils

import bean.BlockData
import bean.BlockSettings
import bean.BlockType
import java.awt.BasicStroke
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.GlyphVector
import java.awt.image.BufferedImage
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object Text2ImageUtils {
  private fun splitToLinesRectangle(text: String, textMetrics: FontMetrics, width: Int): List<String> {
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

  private fun splitToLinesOval(text: String, textMetrics: FontMetrics, width: Int, height: Int): List<String> {
    val lines = text.lines()
    val result = mutableListOf<String>()
    val centerY = height / 2

    var currentY = 0
    lines.flatMap { line ->
      val words = line.split(" ")
      var nIndex = 0

      while (nIndex < words.size && currentY < height) {
        var line = words[nIndex++]
        val adjustedWidth = (width * sqrt(1.0 - ((currentY - centerY).toDouble() / centerY).pow(2.0))).toInt()

        while (nIndex < words.size && textMetrics.stringWidth(line + " " + words[nIndex]) < adjustedWidth) {
          line += " " + words[nIndex]
          nIndex++
        }

        result.add(line)
        currentY += textMetrics.height
      }

      result
    }

    return result
  }

  private fun splitToLines(textMetrics: FontMetrics, blockData: BlockData): List<String> {
    return when (blockData.blockType) {
      is BlockType.Rectangle -> {
        val width = blockData.blockType.width - blockData.settings!!.border * 2
        splitToLinesRectangle(blockData.text, textMetrics, width)
      }

      is BlockType.Oval -> {
        val width = blockData.blockType.width - blockData.settings!!.border * 2
        val height = blockData.blockType.height - blockData.settings!!.border * 2
        splitToLinesOval(blockData.text, textMetrics, width, height)
      }
    }
  }

  fun textToImage(
    globalSettings: BlockSettings,
    blockData: BlockData
  ): Text2ImageResult {
    val x: Int
    val y: Int
    val width: Int
    val height: Int
    when (blockData.blockType) {
      is BlockType.Rectangle -> {
        x = blockData.blockType.x
        y = blockData.blockType.y
        width = blockData.blockType.width
        height = blockData.blockType.height
      }
      is BlockType.Oval -> {
        x = blockData.blockType.x
        y = blockData.blockType.y
        width = blockData.blockType.width
        height = blockData.blockType.height
      }
    }
    val settings = blockData.settings ?: globalSettings

    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics2D = image.createGraphics()

    graphics2D.font = settings.font
    val fontMetrics = graphics2D.fontMetrics
    val textX = x + settings.border
    val textWidth = width - settings.border * 2
    val outlineStroke = BasicStroke(settings.outlineSize.toFloat())

    val lines = splitToLines(fontMetrics, blockData)
    val isOutOfBorder = lines.size * fontMetrics.height + settings.border > height

    // background
    val backgroundColor = settings.backgroundColor
    if (backgroundColor.a != 0) {
      graphics2D.color = backgroundColor.color

      when (blockData.blockType) {
        is BlockType.Rectangle -> {
          val rectangleHeight = min(lines.size * fontMetrics.height + settings.border * 2, height)
          graphics2D.fillRect(x, y, width, rectangleHeight)
        }

        is BlockType.Oval -> {
          val ovalHeight = min(lines.size * fontMetrics.height + settings.border * 2, height)
          graphics2D.fillOval(x, y, width, ovalHeight)
        }
      }
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
    return Text2ImageResult(image, isOutOfBorder)
  }

  fun createSample(
    width: Int,
    height: Int,
    settings: BlockSettings
  ): BufferedImage {
    val text =
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec quis quam tempor, tempus lorem id, aliquet ante. Ut vitae mi blandit, tincidunt arcu eu, mattis metus. Donec eget tincidunt quam, nec tempor ligula. Donec ornare mi nisl, quis imperdiet erat laoreet quis. Phasellus sed neque non sem consequat pharetra. Fusce ultrices erat nec tincidunt vehicula. Duis non odio pharetra, laoreet ipsum varius, porta libero."
    val blockData = BlockData(
      blockType = BlockType.Oval(0, 0, width, height),
      text = text,
      settings = settings
    )
    return textToImage(settings, blockData).image
  }

  class Text2ImageResult(
    val image: BufferedImage,
    val isOutOfBorder: Boolean
  )
}