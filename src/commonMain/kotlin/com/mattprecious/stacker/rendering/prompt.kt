package com.mattprecious.stacker.rendering

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.ajalt.clikt.core.CliktCommand
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.runMosaicBlocking
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import com.mattprecious.stacker.rendering.Ansi.reset
import com.mattprecious.stacker.rendering.Ansi.underline
import kotlinx.coroutines.awaitCancellation
import platform.posix.exit

fun <T> CliktCommand.interactivePrompt(
	message: String,
	options: List<T>,
	filteringEnabled: Boolean = true,
	promptIfSingle: Boolean = false,
	default: T? = null,
	displayTransform: (T) -> String = { it.toString() },
	valueTransform: (T) -> String = { it.toString() },
): T {
	require(options.isNotEmpty())
	if (!promptIfSingle && options.size == 1) {
		return options.single()
	}

	val labelSuffix = if (message.last().isLetterOrDigit()) ": " else " "

	var highlighted by mutableStateOf(default?.let(options::indexOf)?.coerceAtLeast(0) ?: 0)
	var selected by mutableStateOf<Int?>(null)
	var filter by mutableStateOf("")
	var filteredOptions by mutableStateOf(options)

	fun updateFilter(newFilter: String) {
		if (newFilter == filter) return
		filter = newFilter
		filteredOptions = options.filter { valueTransform(it).contains(filter) }
		highlighted = highlighted.coerceIn(0, (filteredOptions.size - 1).coerceAtLeast(0))
	}

	runMosaicBlocking {
		Column(
			modifier = Modifier
				.onKeyEvent {
					if (it.ctrl && it.key == "c") exit(0)

					when {
						it.key == "Enter" -> {
							if (filteredOptions.isNotEmpty()) {
								selected = highlighted
							}

							true
						}

						it.key == "ArrowDown" -> {
							highlighted = (highlighted + 1).coerceAtMost(filteredOptions.size - 1)
							true
						}

						it.key == "ArrowUp" -> {
							highlighted = (highlighted - 1).coerceAtLeast(0)
							true
						}

						filteringEnabled && it.key == "Backspace" -> {
							updateFilter(filter.dropLast(1))
							true
						}

						filteringEnabled && it.key.singleOrNull()?.code in 32..126 -> {
							updateFilter(filter + it.key.single())
							true
						}

						else -> false
					}
				},
		) {
			if (selected == null) {
				Text("$message$labelSuffix$filter")

				filteredOptions.forEachIndexed { index, option ->
					val display = displayTransform(option)
					if (highlighted == index) {
						Text("❯ $underline$display$reset")
					} else {
						Text("  $display")
					}
				}

				LaunchedEffect(Unit) {
					awaitCancellation()
				}
			} else {
				Text("$message$labelSuffix${valueTransform(filteredOptions[selected!!])}")
			}
		}
	}

	val result = filteredOptions[selected!!]
	return result
}
