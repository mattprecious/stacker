package com.mattprecious.stacker.command.branch

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.branchDown(): StackerCommand {
	return BranchDown(
		configManager = configManager,
		locker = locker,
		stackManager = stackManager,
		vc = vc,
	)
}

internal class BranchDown(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val parent = stackManager.getBranch(vc.currentBranchName)!!.parent
		if (parent != null) {
			vc.checkout(parent.name)
		}
	}
}
