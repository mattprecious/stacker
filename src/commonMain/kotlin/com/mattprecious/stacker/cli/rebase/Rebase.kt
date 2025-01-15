package com.mattprecious.stacker.cli.rebase

import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.rebase.rebaseAbort
import com.mattprecious.stacker.command.rebase.rebaseContinue

internal class Rebase(
	private val stacker: StackerDeps,
) : StackerCliktCommand() {
	private val abort: Boolean by option().flag()
	private val cont: Boolean by option("--continue").flag()

	override val command: StackerCommand
		get() = when {
			abort -> stacker.rebaseAbort()
			cont -> stacker.rebaseContinue()
			else -> throw PrintHelpMessage(currentContext, error = true)
		}
}
