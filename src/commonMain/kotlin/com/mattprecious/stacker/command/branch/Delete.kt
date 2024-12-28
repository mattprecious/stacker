package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.mattprecious.stacker.command.StackerCliktCommand
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Delete(
	configManager: ConfigManager,
	locker: Locker,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCliktCommand(shortAlias = "dl") {
	private val branchName: String? by argument().optional()

	override val command by lazy {
		DeleteCommand(
			branchName = branchName,
			configManager = configManager,
			locker = locker,
			stackManager = stackManager,
			vc = vc,
		)
	}
}

internal class DeleteCommand(
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
