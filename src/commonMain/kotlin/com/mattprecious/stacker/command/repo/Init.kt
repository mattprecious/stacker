package com.mattprecious.stacker.command.repo

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.interactivePrompt
import com.mattprecious.stacker.vc.VersionControl

internal class Init(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val vc: VersionControl,
) : StackerCommand() {
	override fun run() {
		requireNoLock(locker)

		val (currentTrunk, currentTrailingTrunk) = if (configManager.repoInitialized) {
			configManager.trunk to configManager.trailingTrunk
		} else {
			null to null
		}

		val branches = vc.branches
		if (branches.isEmpty()) {
			// We need a SHA in order to initialize. Additionally, I don't know how to get the current branch name when it's
			// an unborn branch.
			echo(
				message = "Stacker cannot be initialized in a completely empty repository. Please make a commit, first.",
				err = true,
			)
			throw Abort()
		}

		val defaultTrunk = run defaultTrunk@ {
			if (currentTrunk != null) return@defaultTrunk currentTrunk

			if (branches.size == 1) return@defaultTrunk null

			val defaultBranch = vc.defaultBranch
			if (defaultBranch != null && branches.contains(defaultBranch)) {
				return@defaultTrunk defaultBranch
			}

			return@defaultTrunk vc.currentBranchName
		}

		val trunk = interactivePrompt(
			message = "Select your trunk branch, which you open pull requests against",
			options = branches,
			default = defaultTrunk,
		)

		val trunkSha = vc.getSha(trunk)

		val useTrailing = YesNoPrompt(
			terminal = currentContext.terminal,
			prompt = "Do you use a trailing-trunk workflow?",
			default = currentTrailingTrunk != null,
		).ask() == true

		val trailingTrunk = if (!useTrailing) {
			null
		} else {
			// TODO: This assumes that the trailing trunk branch already exists. It will fail if there's
			//  only one branch in the repo, and will auto-select if there's only two.
			interactivePrompt(
				message = "Select your trailing trunk branch, which you branch from",
				options = branches.filterNot { it == trunk },
				default = currentTrailingTrunk,
			)
		}

		configManager.initializeRepo(trunk = trunk, trunkSha = trunkSha, trailingTrunk = trailingTrunk)
	}
}
