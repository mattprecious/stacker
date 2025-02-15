package com.mattprecious.stacker.command.stack

import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.collections.all
import com.mattprecious.stacker.collections.ancestors
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.requireAuthenticated
import com.mattprecious.stacker.command.submit
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.rendering.code
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.stackSubmit(): StackerCommand {
	return StackSubmit(
		configManager = configManager,
		locker = locker,
		remote = remote,
		stackManager = stackManager,
		vc = vc,
	)
}

internal class StackSubmit(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val remote: Remote,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranch = stackManager.getBranch(vc.currentBranchName)
		if (currentBranch == null) {
			printStaticError(
				buildAnnotatedString {
					append("Cannot create a pull request from ")
					branch { append(vc.currentBranchName) }
					append(" since it is not tracked. Please track with ")
					code { append("st branch track") }
					append(".")
				},
			)
			abort()
		}

		if (currentBranch.name == configManager.trunk || currentBranch.name == configManager.trailingTrunk) {
			printStaticError(
				buildAnnotatedString {
					append("Cannot create a pull request from trunk branch ")
					branch { append(currentBranch.name) }
					append(".")
				},
			)
			abort()
		}

		requireAuthenticated(remote)

		val branchesToSubmit = (currentBranch.ancestors.toList().asReversed() + currentBranch.all)
			.filterNot { it.name == configManager.trunk || it.name == configManager.trailingTrunk }

		vc.pushBranches(branchesToSubmit.map { it.name })
		branchesToSubmit.forEach { it.submit(this, configManager, remote, stackManager, vc) }
	}
}
