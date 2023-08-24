package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.core.Abort
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.error
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Bottom(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "b") {
	override fun run() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val trunk = configManager.trunk
		val trailingTrunk = configManager.trailingTrunk
		val currentBranch = stackManager.getBranch(vc.currentBranchName)!!

		if (currentBranch.name == trailingTrunk || currentBranch.name == trunk) {
			error("Not in a stack.")
			throw Abort()
		}

		var bottom = currentBranch
		while (bottom.parent!!.name != trailingTrunk) {
			bottom = bottom.parent!!
		}

		vc.checkout(bottom.name)
	}
}
