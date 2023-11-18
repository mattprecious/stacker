package com.mattprecious.stacker.rendering

import com.github.ajalt.clikt.core.CliktCommand
import com.mattprecious.stacker.rendering.Ansi.clearEntireLine
import com.mattprecious.stacker.rendering.Ansi.cursorDown
import com.mattprecious.stacker.rendering.Ansi.cursorUp
import com.mattprecious.stacker.rendering.Ansi.reset
import com.mattprecious.stacker.rendering.Ansi.restorePosition
import com.mattprecious.stacker.rendering.Ansi.underline
import org.jline.terminal.TerminalBuilder

fun <T> CliktCommand.interactivePrompt(
	message: String,
	options: List<T>,
	filteringEnabled: Boolean = true,
	default: T? = null,
	displayTransform: (T) -> String = { it.toString() },
	valueTransform: (T) -> String = { it.toString() },
): T {
	require(options.isNotEmpty())
	if (options.size == 1) {
		return options.single()
	}

	val builder = StringBuilder(options.size)
	val outputTerminal = currentContext.terminal

	val labelSuffix = if (message.last().isLetterOrDigit()) ": " else " "

	TerminalBuilder.terminal().use { inputTerminal ->
		var highlighted = default?.let(options::indexOf)?.coerceAtLeast(0) ?: 0
		// Make use of the terminal clearing part of the loop by temporarily holding onto the return value.
		var selected: Int? = null
		var filter = ""
		var filteredOptions = options

		fun updateFilter(newFilter: String) {
			if (newFilter == filter) return
			filter = newFilter
			filteredOptions = options.filter { valueTransform(it).contains(filter) }
			highlighted = highlighted.coerceIn(0, (filteredOptions.size - 1).coerceAtLeast(0))
		}

		while (true) {
			with(builder) {
				if (isNotEmpty()) {
					// TODO: This is wrong if any lines soft wrapped.
					val clearingSize = lines().size
					clear()

					repeat(clearingSize - 1) {
						append(cursorDown)
					}

					repeat(clearingSize - 1) {
						append(clearEntireLine)
						append(cursorUp)
					}

					append(clearEntireLine)
				}

				append(message)
				append(labelSuffix)

				selected.let {
					if (it == null) {
						append(filter)
						append(Ansi.savePosition)
					} else {
						val result = filteredOptions[it]
						appendLine(valueTransform(result))
						outputTerminal.rawPrint(toString())
						return result
					}
				}

				appendLine()

				filteredOptions.forEachIndexed { index, option ->
					val display = displayTransform(option)
					if (highlighted == index) {
						appendLine("❯ $underline$display$reset")
					} else {
						appendLine("  $display")
					}
				}

				append(restorePosition)

				outputTerminal.rawPrint(toString())
			}

			inputTerminal.enterRawMode()

			val reader = inputTerminal.reader()
			while (true) {
				when (val c = reader.read()) {
					10, 13 -> {
						if (filteredOptions.isNotEmpty()) {
							selected = highlighted
						}
						break
					}

					27 -> {
						when (reader.read()) {
							91 -> {
								when (reader.read()) {
									65 -> highlighted = (highlighted - 1).coerceAtLeast(0)
									66 -> highlighted = (highlighted + 1).coerceAtMost(filteredOptions.size - 1)
								}

								break
							}
						}
					}
					in 32..126 -> {
						if (filteringEnabled) {
							updateFilter(filter + c.toChar())
						}
						break
					}
					127 -> {
						if (filteringEnabled) {
							updateFilter(filter.dropLast(1))
						}
						break
					}
				}
			}
		}
	}
}
