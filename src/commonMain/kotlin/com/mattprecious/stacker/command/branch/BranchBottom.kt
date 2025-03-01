package com.mattprecious.stacker.command.branch

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.branchBottom(): StackerCommand {
	return BranchBottom(
		configManager = configManager,
		locker = locker,
		stackManager = stackManager,
		vc = vc,
	)
}

internal class BranchBottom(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val trunk = configManager.trunk
		val trailingTrunk = configManager.trailingTrunk
		val currentBranch = stackManager.getBranch(vc.currentBranchName)!!

		if (currentBranch.name == trailingTrunk || currentBranch.name == trunk) {
			printStatic("Not in a stack.")
			abort()
		}

		var bottom = currentBranch
		while (bottom.parent!!.name != trailingTrunk && bottom.parent!!.name != trunk) {
			bottom = bottom.parent!!
		}

		vc.checkout(bottom.name)
	}
}
