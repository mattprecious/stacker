package com.mattprecious.stacker.command.repo

import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.stack.Branch
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Sync(
	private val configManager: ConfigManager,
	private val remote: Remote,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(
	shortAlias = "s",
) {
	override fun run() {
		val trunk = configManager.trunk!!
		val trailingTrunk = configManager.trailingTrunk

		vc.pull(trunk)
		trailingTrunk?.let(vc::pull)

		stackManager.getBase()!!.offerBranchDeletion { it.name != trunk && it.name != trailingTrunk }
	}

	/**
	 * Recursively checks the status of any remote PRs associated with this branch and offers to delete the branch locally
	 * if PR is in a terminal state.
	 *
	 * Does not remember your selection if you say "no", and will ask again on subsequent calls.
	 */
	private fun Branch.offerBranchDeletion(filter: (Branch) -> Boolean) {
		if (filter(this)) {
			val status = remote.getPrStatus(name)
			val prompt = when (status) {
				Remote.PrStatus.Closed -> "closed"
				Remote.PrStatus.Merged -> "merged"
				Remote.PrStatus.NotFound,
				Remote.PrStatus.Open,
				-> return
			}

			val delete = YesNoPrompt(
				terminal = terminal,
				prompt = "PR for ${name.styleBranch()} has been $prompt, would you like to delete it?",
			).ask()

			if (delete == true) {
				stackManager.untrackBranch(this)
			}
		}

		children.forEach { it.offerBranchDeletion(filter) }
	}
}
