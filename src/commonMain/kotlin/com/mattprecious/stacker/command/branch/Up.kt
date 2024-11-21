package com.mattprecious.stacker.command.branch

import com.mattprecious.stacker.command.StackerMosaicCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.interactivePrompt
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Up(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerMosaicCommand(shortAlias = "u") {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val options = stackManager.getBranch(vc.currentBranchName)!!.children
		val branch = interactivePrompt(
			message = "Move up to",
			options = options,
			displayTransform = { it.name },
			valueTransform = { it.name },
		)

		vc.checkout(branch.name)
	}
}
