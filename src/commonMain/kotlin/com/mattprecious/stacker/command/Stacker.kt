package com.mattprecious.stacker.command

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.command.branch.Branch
import com.mattprecious.stacker.command.downstack.Downstack
import com.mattprecious.stacker.command.log.Log
import com.mattprecious.stacker.command.rebase.Rebase
import com.mattprecious.stacker.command.repo.Repo
import com.mattprecious.stacker.command.stack.Stack
import com.mattprecious.stacker.command.upstack.Upstack
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Stacker(
	configManager: ConfigManager,
	locker: Locker,
	remote: Remote,
	stackManager: StackManager,
	useFancySymbols: Boolean,
	vc: VersionControl,
) : StackerCommand(
	name = "st",
) {
	init {
		stackManager.reconcileBranches(vc)

		subcommands(
			Branch(
				configManager = configManager,
				locker = locker,
				remote = remote,
				stackManager = stackManager,
				useFancySymbols = useFancySymbols,
				vc = vc,
			),
			Downstack(
				configManager = configManager,
				locker = locker,
				stackManager = stackManager,
				vc = vc,
			),
			Log(
				configManager = configManager,
				stackManager = stackManager,
				useFancySymbols = useFancySymbols,
				vc = vc,
			),
			Rebase(
				configManager = configManager,
				locker = locker,
				stackManager = stackManager,
				vc = vc,
			),
			Repo(
				configManager = configManager,
				locker = locker,
				remote = remote,
				stackManager = stackManager,
				vc = vc,
			),
			Stack(
				configManager = configManager,
				locker = locker,
				remote = remote,
				stackManager = stackManager,
				vc = vc,
			),
			Upstack(
				configManager = configManager,
				locker = locker,
				stackManager = stackManager,
				useFancySymbols = useFancySymbols,
				vc = vc,
			),
		)
	}

	override fun run() = Unit

	private fun StackManager.reconcileBranches(
		vc: VersionControl,
	) {
		untrackBranches(vc.checkBranches(trackedBranchNames.toSet()))
	}
}
