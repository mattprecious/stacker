package com.mattprecious.stacker.cli.repo

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.repo.repoInit

internal class Init(
	private val stacker: StackerDeps,
) : StackerCliktCommand() {
	override val command get() = stacker.repoInit()
}
