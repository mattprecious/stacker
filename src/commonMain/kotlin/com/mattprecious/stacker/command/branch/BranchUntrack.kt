package com.mattprecious.stacker.command.branch

import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.branchUntrack(
	branchName: String?,
): StackerCommand {
	return BranchUntrack(
		branchName = branchName,
		configManager = configManager,
		locker = locker,
		stackManager = stackManager,
		vc = vc,
	)
}

internal class BranchUntrack(
	private val branchName: String?,
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val branchName = branchName ?: vc.currentBranchName
		val currentBranch = stackManager.getBranch(branchName)
		if (currentBranch == null) {
			printStaticError(
				buildAnnotatedString {
					append("Branch ")
					branch { append(branchName) }
					append(" is already not tracked.")
				},
			)
			return
		}

		if (currentBranch.children.isNotEmpty()) {
			printStaticError(
				buildAnnotatedString {
					append("Branch ")
					branch { append(branchName) }
					append(" has children. Please retarget or untrack them.")
				},
			)
			abort()
		}

		stackManager.untrackBranch(currentBranch.value)
	}
}
