package com.mattprecious.stacker.cli.branch

import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Bottom(
	private val stacker: Stacker,
) : StackerCliktCommand(shortAlias = "b") {
	override fun runCommand() =	stacker.branchBottom()
}
