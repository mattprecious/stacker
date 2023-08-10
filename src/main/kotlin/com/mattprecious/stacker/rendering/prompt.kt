package com.mattprecious.stacker.rendering

import com.github.ajalt.clikt.core.CliktCommand
import com.mattprecious.stacker.rendering.Ansi.clearEntireLine
import com.mattprecious.stacker.rendering.Ansi.cursorUp
import com.mattprecious.stacker.rendering.Ansi.reset
import com.mattprecious.stacker.rendering.Ansi.underline
import org.jline.terminal.TerminalBuilder

context(CliktCommand)
fun interactivePrompt(
	message: String,
	options: List<String>,
	default: String? = null,
): String {
	require(options.isNotEmpty())
	if (options.size == 1) {
		return options.single()
	}

	val builder = StringBuilder(options.size)
	val outputTerminal = currentContext.terminal

	TerminalBuilder.terminal().use { inputTerminal ->
		var highlighted = default?.let(options::indexOf)?.coerceAtLeast(0) ?: 0
		// Make use of the terminal clearing part of the loop by temporarily holding onto the return value.
		var selected: Int? = null

		while (true) {
			with(builder) {
				if (builder.isNotEmpty()) {
					clear()
					repeat(options.size + 1) {
						append(clearEntireLine)
						append(cursorUp)
					}

					append(clearEntireLine)
				}

				append(message)
				append(':')

				selected?.let {
					val result = options[it]
					appendLine(" $result")
					outputTerminal.rawPrint(toString())
					return result
				}

				appendLine()

				options.forEachIndexed { index, option ->
					if (highlighted == index) {
						appendLine("❯ $underline$option$reset")
					} else {
						appendLine("  $option")
					}
				}

				outputTerminal.rawPrint(toString())
			}

			inputTerminal.enterRawMode()

			val reader = inputTerminal.reader()
			while (true) {
				when (reader.read()) {
					10, 13 -> {
						selected = highlighted
						break
					}

					27 -> {
						when (reader.read()) {
							91 -> {
								when (reader.read()) {
									65 -> highlighted = (highlighted - 1).coerceAtLeast(0)
									66 -> highlighted = (highlighted + 1).coerceAtMost(options.size - 1)
								}

								break
							}
						}
					}
				}
			}
		}
	}
}
