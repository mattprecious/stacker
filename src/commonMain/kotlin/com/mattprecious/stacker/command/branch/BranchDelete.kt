package com.mattprecious.stacker.command.branch

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.branchDelete(
	branchName: String?,
): StackerCommand {
	return BranchDelete(
		branchName = branchName,
		configManager = configManager,
		locker = locker,
		stackManager = stackManager,
		vc = vc,
	)
}

internal class BranchDelete(
	private val branchName: String?,
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranchName = vc.currentBranchName

		val branchName = branchName ?: currentBranchName
		if (branchName == configManager.trunk || branchName == configManager.trailingTrunk) {
			printStaticError("Cannnot delete a trunk branch.")
			abort()
		}

		val branch = stackManager.getBranch(branchName)
		if (branch != null) {
			if (branch.children.isNotEmpty()) {
				printStaticError("Branch has children. Please retarget or untrack them.")
				abort()
			}

			if (branchName == currentBranchName) {
				vc.checkout(branch.parent!!.name)
			}

			stackManager.untrackBranch(branch.value)
		}

		vc.delete(branchName)
	}
}
