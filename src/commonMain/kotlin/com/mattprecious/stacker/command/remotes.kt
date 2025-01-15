package com.mattprecious.stacker.command

import com.github.ajalt.mordant.terminal.ConversionResult
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.rendering.Prompt
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.stack.TreeNode
import com.mattprecious.stacker.vc.VersionControl

internal suspend fun StackerCommandScope.requireAuthenticated(remote: Remote) {
	if (!remote.isAuthenticated) {
		val token = render { onResult ->
			Prompt(
				message = "Please enter a GitHub access token",
				hideInput = true,
				onSubmit = onResult,
			)
		}

		when {
			token.isBlank() -> ConversionResult.Invalid("Cannot be blank.")
			remote.setToken(token) -> ConversionResult.Valid(token)
			else -> ConversionResult.Invalid("Invalid token.")
		}
	}

	if (remote.repoName == null) {
		printStaticError("Unable to parse repository name from origin URL.")
		abort()
	}

	if (!remote.hasRepoAccess) {
		printStaticError("Personal token does not have access to ${remote.repoName}.")
		abort()
	}
}

internal fun TreeNode<Branch>.submit(
	commandScope: StackerCommandScope,
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
		is Remote.PrResult.Created -> commandScope.printStatic("Pull request created: ${result.url}")
		is Remote.PrResult.Updated -> commandScope.printStatic("Pull request updated: ${result.url}")
		is Remote.PrResult.NoChange -> {
			commandScope.printStatic("Pull request already up-to-date: ${result.url}")
		}
	}
}
