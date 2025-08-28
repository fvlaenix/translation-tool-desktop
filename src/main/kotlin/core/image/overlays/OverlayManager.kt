package core.image.overlays

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf

/**
 * Manages overlay rendering order, visibility, and interaction coordination.
 *
 * This centralized manager handles:
 * - Overlay collection and ordering by renderOrder
 * - Overlay visibility management
 * - Overlay lookup and coordination
 * - Future: Input event routing to overlays
 * - Future: Hit testing and overlay selection
 */
@Stable
class OverlayManager {

  private val _overlays = mutableStateListOf<ImageOverlay>()

  /**
   * Read-only list of overlays sorted by render order
   */
  val overlays: List<ImageOverlay> get() = _overlays.sortedBy { it.renderOrder }

  /**
   * Add an overlay to the manager.
   * The overlay will be automatically sorted by render order.
   */
  fun addOverlay(overlay: ImageOverlay) {
    // Remove existing overlay with same ID if present
    _overlays.removeAll { it.id == overlay.id }

    // Add new overlay
    _overlays.add(overlay)
  }

  /**
   * Remove overlay by ID
   */
  fun removeOverlay(id: String) {
    _overlays.removeAll { it.id == id }
  }

  /**
   * Remove all overlays matching the predicate
   */
  fun removeOverlays(predicate: (ImageOverlay) -> Boolean) {
    _overlays.removeAll(predicate)
  }

  /**
   * Get overlay by ID
   */
  fun getOverlay(id: String): ImageOverlay? {
    return _overlays.find { it.id == id }
  }

  /**
   * Get all overlays matching the predicate
   */
  fun getOverlays(predicate: (ImageOverlay) -> Boolean): List<ImageOverlay> {
    return _overlays.filter(predicate)
  }

  /**
   * Check if overlay exists by ID
   */
  fun hasOverlay(id: String): Boolean {
    return _overlays.any { it.id == id }
  }

  /**
   * Get count of overlays
   */
  fun getOverlayCount(): Int = _overlays.size

  /**
   * Get visible overlays only
   */
  fun getVisibleOverlays(): List<ImageOverlay> {
    return overlays.filter { it.isVisible }
  }

  /**
   * Clear all overlays
   */
  fun clearOverlays() {
    _overlays.clear()
  }

  /**
   * Replace all overlays with new list
   */
  fun setOverlays(newOverlays: List<ImageOverlay>) {
    _overlays.clear()
    _overlays.addAll(newOverlays)
  }

  /**
   * Update multiple overlays at once
   */
  fun updateOverlays(overlayUpdates: List<ImageOverlay>) {
    overlayUpdates.forEach { overlay ->
      addOverlay(overlay) // This will replace existing overlay with same ID
    }
  }

  /**
   * Migration helper: Update overlays from box list
   * Maintains existing overlay instances where possible for performance
   */
  fun updateBoxOverlays(
    newBoxes: List<BoxOverlay>,
    replaceAll: Boolean = true
  ) {
    if (replaceAll) {
      // Remove all existing box overlays
      _overlays.removeAll { it.id.startsWith("box_") }
    }

    // Add new overlays
    newBoxes.forEach { overlay ->
      addOverlay(overlay)
    }
  }

  /**
   * Migration helper: Get all box overlays
   */
  fun getBoxOverlays(): List<BoxOverlay> {
    return _overlays.filterIsInstance<BoxOverlay>()
  }

  /**
   * Migration helper: Update box selection
   */
  fun updateBoxSelection(selectedIndex: Int?) {
    // This would ideally update overlay selection state
    // For now, overlays manage their own selection state
    // Future implementation might need centralized selection management
  }

  /**
   * Convenience method to replace overlays by type
   */
  inline fun <reified T : ImageOverlay> replaceOverlaysOfType(newOverlays: List<T>) {
    removeOverlays { it is T }
    newOverlays.forEach { addOverlay(it) }
  }

  /**
   * Debug method to log current overlay state
   */
  fun debugLogOverlays() {
    println("OverlayManager: ${_overlays.size} overlays")
    overlays.forEach { overlay ->
      println("  - ${overlay.id} (order: ${overlay.renderOrder}, visible: ${overlay.isVisible})")
    }
  }
}