package com.mattprecious.stacker.command.repo

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
		val trunk = interactivePrompt(
			message = "Select your trunk branch, which you open pull requests against",
			options = branches,
			// TODO: Infer without hard coding.
			default = currentTrunk ?: "main",
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
			interactivePrompt(
				message = "Select your trailing trunk branch, which you branch from",
				options = branches.filterNot { it == trunk },
				default = currentTrailingTrunk,
			)
		}

		configManager.initializeRepo(trunk = trunk, trunkSha = trunkSha, trailingTrunk = trailingTrunk)
	}
}
