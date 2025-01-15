package com.mattprecious.stacker.cli.branch

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.branch.branchBottom

internal class Bottom(
	private val stacker: StackerDeps,
) : StackerCliktCommand(shortAlias = "b") {
	override val command get() = stacker.branchBottom()
}
