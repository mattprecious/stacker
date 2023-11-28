package com.mattprecious.stacker.rendering

import com.github.ajalt.clikt.core.CliktCommand
import com.mattprecious.stacker.rendering.Ansi.clearEntireLine
import com.mattprecious.stacker.rendering.Ansi.cursorDown
import com.mattprecious.stacker.rendering.Ansi.cursorUp
import com.mattprecious.stacker.rendering.Ansi.reset
import com.mattprecious.stacker.rendering.Ansi.restorePosition
import com.mattprecious.stacker.rendering.Ansi.underline
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.BRKINT
import platform.posix.CS8
import platform.posix.CSIZE
import platform.posix.ECHO
import platform.posix.ECHONL
import platform.posix.ICANON
import platform.posix.ICRNL
import platform.posix.IEXTEN
import platform.posix.IGNBRK
import platform.posix.IGNCR
import platform.posix.INLCR
import platform.posix.ISIG
import platform.posix.ISTRIP
import platform.posix.IXON
import platform.posix.OPOST
import platform.posix.PARENB
import platform.posix.PARMRK
import platform.posix.STDIN_FILENO
import platform.posix.TCSAFLUSH
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

	val builder = StringBuilder(options.size)
	val outputTerminal = currentContext.terminal

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
	// TODO: Figure out ncurses, because this is ridiculous.

	val termios = alloc<termios>()
	check(tcgetattr(STDIN_FILENO, termios.ptr) == 0) {
		"Unable to get the terminal attributes."
	}

	// These flags are sourced from cfmakeraw: https://www.man7.org/linux/man-pages/man3/termios.3.html
	// This can maybe just call cfmakeraw directly, but I don't know how to reset it afterward.
	if (raw) {
		termios.c_iflag =
			termios.c_iflag and (IGNBRK or BRKINT or PARMRK or ISTRIP or INLCR or IGNCR or ICRNL or IXON).inv().convert()
		termios.c_oflag = termios.c_oflag and OPOST.inv().convert()
		termios.c_lflag = termios.c_lflag and (ECHO or ECHONL or ICANON or ISIG or IEXTEN).inv().convert()
		termios.c_cflag = termios.c_cflag and (CSIZE or PARENB).inv().convert() or CS8.convert()
	} else {
		termios.c_iflag =
			termios.c_iflag or (IGNBRK or BRKINT or PARMRK or ISTRIP or INLCR or IGNCR or ICRNL or IXON).convert()
		termios.c_oflag = termios.c_oflag or OPOST.convert()
		termios.c_lflag = termios.c_lflag or (ECHO or ECHONL or ICANON or ISIG or IEXTEN).convert()
		termios.c_cflag = termios.c_cflag or (CSIZE or PARENB).convert() and CS8.inv().convert()
	}

	tcsetattr(STDIN_FILENO, TCSAFLUSH, termios.ptr)
}
