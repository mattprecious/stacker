package com.mattprecious.stacker.command.upstack

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Upstack(
	configManager: ConfigManager,
	locker: Locker,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCommand(shortAlias = "us") {
	init {
		subcommands(
			Onto(configManager, locker, stackManager, vc),
			Restack(configManager, locker, stackManager, vc),
		)
	}

	override fun run() = Unit
}
