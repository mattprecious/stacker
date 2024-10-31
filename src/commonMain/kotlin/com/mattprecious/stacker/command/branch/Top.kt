package com.mattprecious.stacker.command.branch

import com.mattprecious.stacker.command.StackerMosaicCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.interactivePrompt
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.stack.TreeNode
import com.mattprecious.stacker.vc.VersionControl

internal class Top(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerMosaicCommand(shortAlias = "t") {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val options = stackManager.getBranch(vc.currentBranchName)!!.leaves()
		val branch = interactivePrompt(
			message = "Choose which top",
			options = options,
			displayTransform = { it.name },
			valueTransform = { it.name },
		)

		vc.checkout(branch.name)
	}

	private fun TreeNode<Branch>.leaves(): List<TreeNode<Branch>> {
		return if (children.isEmpty()) {
			listOf(this)
		} else {
			children.flatMap { it.leaves() }
		}
	}
}
