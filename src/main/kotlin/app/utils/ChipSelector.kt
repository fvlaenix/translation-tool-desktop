package app.utils

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Copied from https://gist.github.com/fvilarino/997169b74410a7821e393551431f732d
 */

object ChipSelector {
  private const val ANIMATION_DURATION_MILLISECONDS = 600

  enum class SelectionMode(val index: Int) {
    Single(0),
    Multiple(1);

    companion object {
      fun fromIndex(index: Int) = entries.firstOrNull { it.index == index } ?: Single
    }
  }

  @Stable
  interface ChipSelectorState {
    val chips: List<String>
    val selectedChips: List<String>

    fun onChipClick(chip: String)
    fun isSelected(chip: String): Boolean
  }

  open class ChipSelectorStateImpl(
    override val chips: List<String>,
    selectedChips: List<String> = emptyList(),
    val mode: SelectionMode = SelectionMode.Single,
  ) : ChipSelectorState {
    override var selectedChips by mutableStateOf(selectedChips)

    override fun onChipClick(chip: String) {
      if (mode == SelectionMode.Single) {
        if (!selectedChips.contains(chip)) {
          selectedChips = listOf(chip)
        }
      } else {
        selectedChips = if (selectedChips.contains(chip)) {
          selectedChips - chip
        } else {
          selectedChips + chip
        }
      }
    }

    override fun isSelected(chip: String): Boolean = selectedChips.contains(chip)

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as ChipSelectorStateImpl

      if (chips != other.chips) return false
      if (mode != other.mode) return false
      if (selectedChips != other.selectedChips) return false

      return true
    }

    override fun hashCode(): Int {
      var result = chips.hashCode()
      result = 31 * result + mode.hashCode()
      result = 31 * result + selectedChips.hashCode()
      return result
    }

    companion object {
      val saver = Saver<ChipSelectorStateImpl, List<*>>(
        save = { state ->
          buildList {
            add(state.chips.size)
            addAll(state.chips)
            add(state.selectedChips.size)
            addAll(state.selectedChips)
            add(state.mode.index)
          }
        },
        restore = { items ->
          var index = 0
          val chipsSize = items[index++] as Int
          val chips = List(chipsSize) {
            items[index++] as String
          }
          val selectedSize = items[index++] as Int
          val selectedChips = List(selectedSize) {
            items[index++] as String
          }
          val mode = SelectionMode.fromIndex(items[index] as Int)
          ChipSelectorStateImpl(
            chips = chips,
            selectedChips = selectedChips,
            mode = mode,
          )
        }
      )
    }
  }

  @Composable
  fun rememberChipSelectorState(
    chips: List<String>,
    selectedChips: List<String> = emptyList(),
    mode: SelectionMode = SelectionMode.Single,
    onChange: (String) -> Unit = {}
  ): ChipSelectorState {
    if (chips.isEmpty()) error("No chips provided")
    if (mode == SelectionMode.Single && selectedChips.size > 1) {
      error("Single choice can only have 1 pre-selected chip")
    }

    return rememberSaveable(
      saver = ChipSelectorStateImpl.saver
    ) {
      object : ChipSelectorStateImpl(
        chips,
        selectedChips,
        mode,
      ) {
        override fun onChipClick(chip: String) {
          super.onChipClick(chip)
          onChange(chip)
        }
      }
    }
  }

  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  fun ChipsSelector(
    state: ChipSelectorState,
    modifier: Modifier = Modifier,
    selectedTextColor: Color = MaterialTheme.colors.primaryVariant,
    unselectedTextColor: Color = MaterialTheme.colors.secondaryVariant,
    selectedBackgroundColor: Color = MaterialTheme.colors.primary,
    unselectedBackgroundColor: Color = MaterialTheme.colors.secondary,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
  ) {
    FlowRow(
      modifier = modifier,
      horizontalArrangement = horizontalArrangement,
      verticalArrangement = verticalArrangement,
    ) {
      state.chips.forEach { chip ->
        Chip(
          label = chip,
          isSelected = state.isSelected(chip),
          onClick = { state.onChipClick(chip) },
          selectedTextColor = selectedTextColor,
          unselectedTextColor = unselectedTextColor,
          selectedBackgroundColor = selectedBackgroundColor,
          unselectedBackgroundColor = unselectedBackgroundColor,
        )
      }
    }
  }

  @Composable
  fun Chip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTextColor: Color = MaterialTheme.colors.primaryVariant,
    unselectedTextColor: Color = MaterialTheme.colors.secondaryVariant,
    selectedBackgroundColor: Color = MaterialTheme.colors.primary,
    unselectedBackgroundColor: Color = MaterialTheme.colors.secondary,
  ) {
    val interactionSource = remember { MutableInteractionSource() }
    val transition = updateTransition(targetState = isSelected, label = "transition")

    val backgroundColor by transition.animateColor(
      transitionSpec = { tween(durationMillis = ANIMATION_DURATION_MILLISECONDS) },
      label = "backgroundColor"
    ) { selected ->
      if (selected) selectedBackgroundColor else unselectedBackgroundColor
    }
    val textColor by transition.animateColor(
      transitionSpec = { tween(durationMillis = ANIMATION_DURATION_MILLISECONDS) },
      label = "textColor",
    ) { selected ->
      if (selected) selectedTextColor else unselectedTextColor
    }
    val textAlpha by transition.animateFloat(
      transitionSpec = { tween(durationMillis = ANIMATION_DURATION_MILLISECONDS) },
      label = "textAlpha"
    ) { selected ->
      if (selected) 1f else .6f
    }
    Box(
      modifier = modifier
        .background(backgroundColor)
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
      Text(
        text = label,
        color = textColor,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
          .padding(horizontal = 24.dp, vertical = 16.dp)
          .graphicsLayer { alpha = textAlpha }
      )
    }
  }
}