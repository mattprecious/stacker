package com.mattprecious.stacker.command.log

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Log(
	configManager: ConfigManager,
	stackManager: StackManager,
	useFancySymbols: Boolean,
	vc: VersionControl,
) : StackerCommand(shortAlias = "l") {
	init {
		subcommands(
			Short(configManager, stackManager, useFancySymbols, vc),
		)
	}

	override fun run() = Unit
}
