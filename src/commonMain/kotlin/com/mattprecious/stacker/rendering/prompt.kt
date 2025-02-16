package com.mattprecious.stacker.rendering

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.AnnotatedString
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import com.mattprecious.stacker.collections.radiateFrom
import kotlinx.collections.immutable.ImmutableList

@Composable
fun Prompt(
	message: String,
	hideInput: Boolean = false,
	onSubmit: (String) -> Unit,
) {
	Prompt(
		message = remember(message) { buildAnnotatedString { append(message) } },
		hideInput = hideInput,
		onSubmit = onSubmit,
	)
}

@Composable
fun Prompt(
	message: AnnotatedString,
	hideInput: Boolean = false,
	onSubmit: (String) -> Unit,
) {
	val printer = LocalPrinter.current
	var input by remember { mutableStateOf("") }

	val prompt = remember(message) {
		buildAnnotatedString {
			append(message)
			append(": ")
		}
	}

	Row {
		Text(prompt)
		Text(
			modifier = Modifier.onKeyEvent {
				when {
					it.key == "Enter" -> {
						val staticMessage = if (hideInput) {
							prompt
						} else {
							prompt + buildAnnotatedString { append(input) }
						}

						printer.printStatic(staticMessage)
						onSubmit(input)
					}

					it.key == "Backspace" -> input = input.dropLast(1)
					!it.ctrl && it.key.singleOrNull()?.code in 32..126 -> input += it.key.single()
					else -> return@onKeyEvent false
				}

				return@onKeyEvent true
			},
			value = if (hideInput) "" else input,
		)
	}
}

@Composable
fun YesNoPrompt(
	message: String,
	default: Boolean? = null,
	onSubmit: (Boolean) -> Unit,
) {
	YesNoPrompt(
		message = remember(message) { buildAnnotatedString { append(message) } },
		default = default,
		onSubmit = onSubmit,
	)
}

@Composable
fun YesNoPrompt(
	message: AnnotatedString,
	default: Boolean? = null,
	onSubmit: (Boolean) -> Unit,
) {
	val printer = LocalPrinter.current
	var input by remember { mutableStateOf("") }

	val prompt = remember(message, default) {
		buildAnnotatedString {
			append(message)
			append(" ")

			code {
				val yes = if (default == true) "Y" else "y"
				val no = if (default == false) "N" else "n"
				append("[$yes/$no]")
			}

			append(": ")
		}
	}

	fun submit(result: Boolean) {
		val staticMessage = if (input == "") {
			prompt
		} else {
			prompt + buildAnnotatedString { append(input) }
		}

		printer.printStatic(staticMessage)
		onSubmit(result)
	}

	Row {
		Text(prompt)
		Text(
			modifier = Modifier.onKeyEvent {
				when {
					it.key == "Enter" -> when (input) {
						"y", "Y" -> submit(true)
						"n", "N" -> submit(false)
						"" -> if (default != null) {
							submit(default)
						}
						else -> input = ""
					}

					it.key == "Backspace" -> input = input.dropLast(1)
					!it.ctrl && it.key.singleOrNull()?.code in 32..126 -> input += it.key.single()
					else -> return@onKeyEvent false
				}

				return@onKeyEvent true
			},
			value = input,
		)
	}
}

@Stable
class PromptState<T>(
	private val options: ImmutableList<T>,
	default: T?,
	private val displayTransform: (T) -> String,
	private val valueTransform: (T) -> String,
	private val optionKey: (T) -> Any? = { it },
) {
	@Immutable
	internal inner class Option(
		val option: T,
	) {
		val display: String get() = displayTransform(option)
		val value: String get() = valueTransform(option)
	}

	// Highlighting logic when filtering or unfiltering is:
	// - When appending to the filter, maintain the currently highlighted option if it remains in the
	//   newly filtered list.
	// - If it is no longer visible, move highlight to the closest remaining option.
	//   - In the case of a tie, prefer the earlier (left/top) option.
	// - When popping from the filter, restore the highlight to what it was for this filter
	//   previously, unless an arrow key was pressed at any point since the filter was appended.
	var highlighted by mutableStateOf(default?.let(options::indexOf)?.coerceAtLeast(0) ?: 0)
		private set

	private val highlightedStack = mutableStateListOf<Int>()

	internal var filter by mutableStateOf("")
		private set

	internal val filteredOptions by derivedStateOf {
		options
			.map(::Option)
			.filter { it.value.contains(filter) }
	}

	init {
		require(options.isNotEmpty())
	}

	internal fun moveUp() {
		if (filteredOptions.isEmpty()) return
		highlighted = (highlighted - 1).coerceAtLeast(0)
		highlightedStack.clear()
	}

	internal fun moveDown() {
		if (filteredOptions.isEmpty()) return
		highlighted = (highlighted + 1).coerceAtMost(filteredOptions.size - 1)
		highlightedStack.clear()
	}

	internal fun filterAppend(char: Char) {
		val prioritizedNextHighlight = if (filteredOptions.isEmpty()) {
			null
		} else {
			filteredOptions.radiateFrom(highlighted)
		}

		highlightedStack.add(highlighted)
		filter += char

		highlighted = prioritizedNextHighlight?.firstNotNullOfOrNull { nextPrioritized ->
			filteredOptions
				.indexOfFirst { optionKey(it.option) == optionKey(nextPrioritized.option) }
				.takeIf { it != -1 }
		} ?: 0
	}

	internal fun filterDrop() {
		val lastHighlighted = filteredOptions.getOrNull(highlighted)?.option
		filter = filter.dropLast(1)

		highlighted = when {
			highlightedStack.isNotEmpty() -> highlightedStack.removeLast()
			lastHighlighted != null -> {
				val key = optionKey(lastHighlighted)
				filteredOptions
					.indexOfFirst { optionKey(it.option) == key }
					.coerceAtLeast(0)
			}

			else -> 0
		}
	}

	internal fun select(): Option? {
		return if (filteredOptions.isNotEmpty()) {
			filteredOptions[highlighted]
		} else {
			null
		}
	}
}

@Composable
fun <T> InteractivePrompt(
	message: String,
	state: PromptState<T>,
	filteringEnabled: Boolean = true,
	onSelected: (T) -> Unit,
) {
	InteractivePrompt(
		message = remember(message) { buildAnnotatedString { append(message) } },
		state = state,
		filteringEnabled = filteringEnabled,
		onSelected = onSelected,
	)
}

@Composable
fun <T> InteractivePrompt(
	message: AnnotatedString,
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

		state.filteredOptions.forEachIndexed { index, option ->
			val text = buildAnnotatedString {
				val styleIndex = if (index == state.highlighted) {
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
