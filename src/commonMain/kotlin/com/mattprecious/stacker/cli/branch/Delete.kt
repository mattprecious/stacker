package com.mattprecious.stacker.cli.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.branch.branchDelete

internal class Delete(
	private val stacker: StackerDeps,
) : StackerCliktCommand(shortAlias = "dl") {
	private val branchName: String? by argument().optional()

	override val command get() = stacker.branchDelete(branchName)
}
