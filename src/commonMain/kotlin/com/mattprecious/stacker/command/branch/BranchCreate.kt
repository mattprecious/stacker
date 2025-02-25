package com.mattprecious.stacker.command.branch

import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.rendering.code
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl
import com.mattprecious.stacker.vc.VersionControl.BranchCreateResult

fun StackerDeps.branchCreate(
	branchName: String,
): StackerCommand {
	return BranchCreate(
		branchName = branchName,
		configManager = configManager,
		locker = locker,
		stackManager = stackManager,
		vc = vc,
	)
}

internal class BranchCreate(
	private val branchName: String,
	private val configManager: ConfigManager,
	private val locker: Locker,
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
					append("Cannot branch from ")
					branch { append(vc.currentBranchName) }
					append(" since it is not tracked. Please track with ")
					code { append("st branch track") }
					append(".")
				},
			)
			abort()
		}

		when (vc.createBranchFromCurrent(branchName)) {
			BranchCreateResult.Success -> {
				stackManager.trackBranch(branchName, currentBranch.name, vc.getSha(currentBranch.name))
			}
			BranchCreateResult.AlreadyExists -> {
				printStaticError(
					buildAnnotatedString {
						append("Branch ")
						branch { append(branchName) }
						append(" already exists.")
					},
				)

				abort()
			}
		}
	}
}
