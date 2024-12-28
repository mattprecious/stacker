package com.mattprecious.stacker.command.stack

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.command.StackerCliktCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Stack(
	configManager: ConfigManager,
	locker: Locker,
	remote: Remote,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCliktCommand(shortAlias = "s") {
	init {
		subcommands(
			Submit(configManager, locker, remote, stackManager, vc),
		)
	}
}
