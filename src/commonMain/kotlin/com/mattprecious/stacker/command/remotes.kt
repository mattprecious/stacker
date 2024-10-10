package com.mattprecious.stacker.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.prompt
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.stack.TreeNode
import com.mattprecious.stacker.vc.VersionControl

internal fun CliktCommand.requireAuthenticated(remote: Remote) {
	if (!remote.isAuthenticated) {
		terminal.prompt(
			prompt = "Please enter a GitHub access token",
			hideInput = true,
		) {
			when {
				it.isBlank() -> ConversionResult.Invalid("Cannot be blank.")
				remote.setToken(it) -> ConversionResult.Valid(it)
				else -> ConversionResult.Invalid("Invalid token.")
			}
		}
	}

	if (remote.repoName == null) {
		echo("Unable to parse repository name from origin URL.", err = true)
		throw Abort()
	}

	if (!remote.hasRepoAccess) {
		echo("Personal token does not have access to ${remote.repoName}.", err = true)
		throw Abort()
	}
}

internal fun TreeNode<Branch>.submit(
	command: CliktCommand,
	configManager: ConfigManager,
	remote: Remote,
	stackManager: StackManager,
	vc: VersionControl,
) {
	val target = parent!!.name.let {
		if (it == configManager.trailingTrunk) {
			configManager.trunk!!
		} else {
			it
		}
	}

	val result = remote.openOrRetargetPullRequest(
		branchName = name,
		targetName = target,
	) {
		// TODO: Figure out what to put when there's multiple commits on this branch.
		val info = vc.latestCommitInfo(name)
		Remote.PrInfo(
			title = info.title,
			body = info.body,
		)
	}

	stackManager.updatePrNumber(value, result.number)

	when (result) {
		is Remote.PrResult.Created -> command.echo("Pull request created: ${result.url}")
		is Remote.PrResult.Updated -> command.echo("Pull request updated: ${result.url}")
		is Remote.PrResult.NoChange -> command.echo("Pull request already up-to-date: ${result.url}")
	}
}
