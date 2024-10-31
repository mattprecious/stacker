package com.mattprecious.stacker.rendering

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.ajalt.clikt.core.CliktCommand
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.runMosaicBlocking
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import kotlinx.coroutines.awaitCancellation
import platform.posix.exit

fun <T> CliktCommand.interactivePrompt(
	message: String,
	options: List<T>,
	filteringEnabled: Boolean = true,
	default: T? = null,
	promptIfSingle: Boolean = false,
	displayTransform: (T) -> String = { it.toString() },
	valueTransform: (T) -> String = { it.toString() },
): T {
	require(options.isNotEmpty())
	if (!promptIfSingle && options.size == 1) {
		return options.first()
	}

	var selected by mutableStateOf<T?>(null)

	runMosaicBlocking {
		LocalPrinter.current.Messages()

		val state = remember {
			PromptState(
				options = options,
				default = default,
				displayTransform = displayTransform,
				valueTransform = valueTransform,
			)
		}

		if (selected == null) {
			InteractivePrompt(
				message = message,
				state = state,
				filteringEnabled = filteringEnabled,
				onSelected = { selected = it },
			)

			LaunchedEffect(Unit) {
				awaitCancellation()
			}
		}
	}

	return selected!!
}

@Stable
class PromptState<T>(
	options: List<T>,
	default: T?,
	private val displayTransform: (T) -> String,
	private val valueTransform: (T) -> String,
) {
	@Immutable
	internal inner class Option(
		val option: T,
		val highlighted: Boolean,
	) {
		val display: String get() = displayTransform(option)
		val value: String get() = valueTransform(option)
	}

	private var highlighted by mutableStateOf(default?.let(options::indexOf)?.coerceAtLeast(0) ?: 0)

	private var _filter by mutableStateOf("")
	internal val filter: String get() = _filter

	internal val filteredOptions by derivedStateOf {
		options
			.mapIndexed { index, option ->
				Option(
					option = option,
					highlighted = index == highlighted,
				)
			}
			.filter { it.value.contains(filter) }
	}

	init {
		require(options.isNotEmpty())
	}

	internal fun moveUp() {
		highlighted = (highlighted - 1).coerceAtLeast(0)
	}

	internal fun moveDown() {
		highlighted = (highlighted + 1).coerceAtMost(filteredOptions.size - 1)
	}

	internal fun filterAppend(char: Char) {
		updateFilter(_filter + char)
	}

	internal fun filterDrop() {
		updateFilter(_filter.dropLast(1))
	}

	internal fun select(): Option? {
		return if (filteredOptions.isNotEmpty()) {
			filteredOptions[highlighted]
		} else {
			null
		}
	}

	private fun updateFilter(newFilter: String) {
		if (newFilter == _filter) return
		_filter = newFilter
		highlighted = highlighted.coerceIn(0, (filteredOptions.size - 1).coerceAtLeast(0))
	}
}

@Composable
fun <T> InteractivePrompt(
	message: String,
	state: PromptState<T>,
	filteringEnabled: Boolean = true,
	onSelected: (T) -> Unit,
) {
	val printer = LocalPrinter.current
	val prompt = remember(message) {
		"$message${if (message.last().isLetterOrDigit()) ":" else ""}"
	}

	Column(
		modifier = Modifier
			.onKeyEvent {
				// TODO: Remove once mosaic is pushed all the way to the top.
				if (it.ctrl && it.key == "c") exit(0)

				when {
					it.key == "Enter" -> {
						state.select()?.let { selected ->
							printer.printStatic("$prompt ${selected.value}")
							onSelected(selected.option)
						}

						true
					}

					it.key == "ArrowDown" -> {
						state.moveDown()
						true
					}

					it.key == "ArrowUp" -> {
						state.moveUp()
						true
					}

					filteringEnabled && it.key == "Backspace" -> {
						state.filterDrop()
						true
					}

					!it.ctrl && filteringEnabled && it.key.singleOrNull()?.code in 32..126 -> {
						state.filterAppend(it.key.single())
						true
					}

					else -> false
				}
			},
	) {
		Text("$prompt ${state.filter}")

		state.filteredOptions.forEach { option ->
			val text = buildAnnotatedString {
				val styleIndex = if (option.highlighted) {
					append("❯ ")
					pushStyle(SpanStyle(textStyle = TextStyle.Underline))
				} else {
					append("  ")
					null
				}

				append(option.display)
				styleIndex?.let(::pop)
			}

			Text(text)
		}
	}
}
