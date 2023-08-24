package com.mattprecious.stacker.command.branch

import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.interactivePrompt
import com.mattprecious.stacker.stack.Branch
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Top(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "t") {
	override fun run() {
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

	private fun Branch.leaves(): List<Branch> {
		return if (children.isEmpty()) {
			listOf(this)
		} else {
			children.flatMap { it.leaves() }
		}
	}
}
