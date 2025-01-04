package com.mattprecious.stacker.cli.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Untrack(
	private val stacker: Stacker,
) : StackerCliktCommand(shortAlias = "ut") {
	private val branchName: String? by argument().optional()

	override suspend fun runCommand() = stacker.branchUntrack(branchName)
}
