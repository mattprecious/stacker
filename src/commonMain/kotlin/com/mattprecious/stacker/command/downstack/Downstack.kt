package com.mattprecious.stacker.command.downstack

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.command.StackerCliktCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Downstack(
	configManager: ConfigManager,
	locker: Locker,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCliktCommand(shortAlias = "ds") {
	init {
		subcommands(
			Edit(configManager, locker, stackManager, vc),
		)
	}
}
