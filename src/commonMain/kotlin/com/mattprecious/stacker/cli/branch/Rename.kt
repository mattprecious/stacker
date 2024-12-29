package com.mattprecious.stacker.cli.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Rename(
	private val stacker: Stacker,
) : StackerCliktCommand(shortAlias = "rn") {
	private val newName by argument()

	override fun runCommand() = stacker.branchRename(newName)
}
