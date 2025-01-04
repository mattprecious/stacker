package com.mattprecious.stacker.cli.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Create(
	private val stacker: Stacker,
) : StackerCliktCommand(shortAlias = "c") {
	private val branchName by argument()

	override suspend fun runCommand() = stacker.branchCreate(branchName)
}
