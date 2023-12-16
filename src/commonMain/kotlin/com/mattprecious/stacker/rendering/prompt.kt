package com.mattprecious.stacker.rendering

import com.github.ajalt.clikt.core.CliktCommand
import com.mattprecious.stacker.rendering.Ansi.clearEntireLine
import com.mattprecious.stacker.rendering.Ansi.cursorDown
import com.mattprecious.stacker.rendering.Ansi.cursorUp
import com.mattprecious.stacker.rendering.Ansi.reset
import com.mattprecious.stacker.rendering.Ansi.restorePosition
import com.mattprecious.stacker.rendering.Ansi.savePosition
import com.mattprecious.stacker.rendering.Ansi.underline
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.ECHO
import platform.posix.ICANON
import platform.posix.ICRNL
import platform.posix.IEXTEN
import platform.posix.INLCR
import platform.posix.ISIG
import platform.posix.IXON
import platform.posix.STDIN_FILENO
import platform.posix.TCSAFLUSH
import platform.posix.exit
import platform.posix.getchar
import platform.posix.tcgetattr
import platform.posix.tcsetattr
import platform.posix.termios

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

	val builder = StringBuilder()

	val labelSuffix = if (message.last().isLetterOrDigit()) ": " else " "

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
					append(savePosition)
				} else {
					val result = filteredOptions[it]
					appendLine(valueTransform(result))
					print(toString())
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

			print(toString())
		}

		withRaw {
			while (true) {
				when (val c = getchar()) {
					10, 13 -> {
						if (filteredOptions.isNotEmpty()) {
							selected = highlighted
						}
						break
					}

					27 -> {
						when (getchar()) {
							91 -> {
								when (getchar()) {
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

					// SIGINT.
					3 -> {
						updateTerminalFlags(raw = false)
						exit(0)
					}
				}
			}
		}
	}
}

private inline fun withRaw(block: () -> Unit) {
	try {
		updateTerminalFlags(raw = true)
		block()
	} finally {
		updateTerminalFlags(raw = false)
	}
}

private fun updateTerminalFlags(raw: Boolean) = memScoped {
	val termios = alloc<termios>()
	check(tcgetattr(STDIN_FILENO, termios.ptr) == 0) {
		"Unable to get the terminal attributes."
	}

	// There seems to be no real consensus on which set of flags are set for raw mode... The ones chosen here are the ones
	// that JLine uses (plus ISIG) because they work, and other sets (like from linux cfmakeraw) do not.
	//
	// ISIG is added because we manually handle it in our input loop in order to reset these flags before terminating.
	// Otherwise, the terminal is left in raw mode.
	//
	// I would love for all of this to be replaced with curses raw/noraw, but I cannot figure out how to make it work.
	if (raw) {
		termios.c_iflag = termios.c_iflag and (INLCR or ICRNL or IXON).inv().convert()
		termios.c_lflag = termios.c_lflag and (ECHO or ICANON or ISIG or IEXTEN).inv().convert()
	} else {
		termios.c_iflag = termios.c_iflag or (INLCR or ICRNL or IXON).convert()
		termios.c_lflag = termios.c_lflag or (ECHO or ICANON or ISIG or IEXTEN).convert()
	}

	tcsetattr(STDIN_FILENO, TCSAFLUSH, termios.ptr)
}
