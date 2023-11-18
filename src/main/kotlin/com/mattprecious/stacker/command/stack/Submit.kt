package com.mattprecious.stacker.command.stack

import com.github.ajalt.clikt.core.Abort
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.requireAuthenticated
import com.mattprecious.stacker.command.submit
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.stack.Branch
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Submit(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val remote: Remote,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "s") {
	override fun run() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranch = stackManager.getBranch(vc.currentBranchName)
		if (currentBranch == null) {
			echo(
				message = "Cannot create a pull request from ${vc.currentBranchName.styleBranch()} since it is " +
					"not tracked. Please track with ${"st branch track".styleCode()}.",
				err = true,
			)
			throw Abort()
		}

		if (currentBranch.name == configManager.trunk || currentBranch.name == configManager.trailingTrunk) {
			echo(
				message = "Cannot create a pull request from trunk branch ${currentBranch.name.styleBranch()}.",
				err = true,
			)
			throw Abort()
		}

		requireAuthenticated(remote)

		val branchesToSubmit = currentBranch.flattenStack()
			.filterNot { it.name == configManager.trunk || it.name == configManager.trailingTrunk }
		vc.pushBranches(branchesToSubmit.map { it.name })
		branchesToSubmit.forEach { it.submit(this, configManager, remote, vc) }
	}

	private fun Branch.flattenStack(): List<Branch> {
		return buildList {
			fun Branch.addParents() {
				if (parent != null) {
					parent!!.addParents()
					add(parent!!)
				}
			}

			fun Branch.addChildren() {
				children.forEach {
					add(it)
					it.addChildren()
				}
			}

			addParents()
			add(this@flattenStack)
			addChildren()
		}
	}
}
