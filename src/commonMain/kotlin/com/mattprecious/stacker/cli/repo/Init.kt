package com.mattprecious.stacker.cli.repo

import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Init(
	private val stacker: Stacker,
) : StackerCliktCommand() {
	override suspend fun runCommand() = stacker.repoInit()
}
