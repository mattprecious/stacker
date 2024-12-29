package com.mattprecious.stacker.cli.rebase

import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Rebase(
	private val stacker: Stacker,
) : StackerCliktCommand() {
	private val abort: Boolean by option().flag()
	private val cont: Boolean by option("--continue").flag()

	override fun runCommand(): Boolean {
		return when {
			abort -> stacker.rebaseAbort()
			cont -> stacker.rebaseContinue()
			else -> throw PrintHelpMessage(currentContext, error = true)
		}
	}
}
