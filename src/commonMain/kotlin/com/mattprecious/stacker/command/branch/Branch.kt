package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Branch(
	configManager: ConfigManager,
	locker: Locker,
	remote: Remote,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCommand(shortAlias = "b") {
	init {
		subcommands(
			Bottom(configManager, locker, stackManager, vc),
			Checkout(configManager, locker, stackManager, vc),
			Create(configManager, locker, stackManager, vc),
			Delete(configManager, locker, stackManager, vc),
			Down(configManager, locker, stackManager, vc),
			Rename(configManager, locker, stackManager, vc),
			Restack(configManager, locker, stackManager, vc),
			Submit(configManager, locker, remote, stackManager, vc),
			Top(configManager, locker, stackManager, vc),
			Track(configManager, locker, stackManager, vc),
			Untrack(configManager, locker, stackManager, vc),
			Up(configManager, locker, stackManager, vc),
		)
	}

	override fun run() = Unit
}
