package com.mattprecious.stacker.cli.upstack

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.upstack.upstackOnto

internal class Onto(
	private val stacker: StackerDeps,
) : StackerCliktCommand(shortAlias = "o") {
	override val command get() = stacker.upstackOnto()
}
