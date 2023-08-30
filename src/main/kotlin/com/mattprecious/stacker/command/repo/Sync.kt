package com.mattprecious.stacker.command.repo

import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.vc.VersionControl

internal class Sync(
	private val configManager: ConfigManager,
	private val vc: VersionControl,
) : StackerCommand(
	shortAlias = "s",
) {
	override fun run() {
		vc.pull(configManager.trunk!!)
		configManager.trailingTrunk?.let(vc::pull)

		// TODO: Delete branches.
	}
}
