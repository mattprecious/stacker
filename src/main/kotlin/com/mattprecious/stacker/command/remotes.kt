package com.mattprecious.stacker.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.ConversionResult
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.stack.Branch
import com.mattprecious.stacker.vc.VersionControl

context(CliktCommand)
internal fun Remote.requireAuthenticated() {
	if (!isAuthenticated) {
		terminal.prompt(
			prompt = "Please enter a GitHub access token",
			hideInput = true,
		) {
			when {
				it.isBlank() -> ConversionResult.Invalid("Cannot be blank.")
				setToken(it) -> ConversionResult.Valid(it)
				else -> ConversionResult.Invalid("Invalid token.")
			}
		}
	}

	if (repoName == null) {
		echo("Unable to parse repository name from origin URL.", err = true)
		throw Abort()
	}

	if (!hasRepoAccess) {
		echo("Personal token does not have access to $repoName.", err = true)
		throw Abort()
	}
}

context(CliktCommand)
internal fun Branch.submit(
	configManager: ConfigManager,
	remote: Remote,
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

	when (result) {
		is Remote.PrResult.Created -> echo("Pull request created: ${result.url}")
		is Remote.PrResult.Updated -> echo("Pull request updated: ${result.url}")
	}
}
