package com.mattprecious.stacker.command.repo

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.vc.VersionControl

internal class Repo(
	configManager: ConfigManager,
	locker: Locker,
	vc: VersionControl,
) : StackerCommand(shortAlias = "r") {
	init {
		subcommands(
			Init(configManager, locker, vc),
			Sync(configManager, vc),
		)
	}

	override fun run() = Unit
}
