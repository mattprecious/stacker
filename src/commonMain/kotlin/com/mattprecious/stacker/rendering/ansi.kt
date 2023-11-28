package com.mattprecious.stacker.rendering

object Ansi {
	const val clearEntireLine = "\u001B[2K"
	const val cursorDown = "\u001b[B"
	const val cursorUp = "\u001B[F"
	const val underline = "\u001B[4m"
	const val reset = "\u001B[0m"
	const val savePosition = "\u001B[s"
	const val restorePosition = "\u001B[u"
}
