package com.mattprecious.stacker

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.ConversionResult
import com.mattprecious.stacker.command.Stacker
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.config.RealConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.lock.RealLocker
import com.mattprecious.stacker.remote.GitHubRemote
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.shell.RealShell
import com.mattprecious.stacker.stack.RealStackManager
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.GitVersionControl
import com.mattprecious.stacker.vc.VersionControl
import java.lang.foreign.Arena
import kotlin.io.path.div
import com.mattprecious.stacker.stack.Branch as StackBranch

context(CliktCommand)
fun error(message: String) {
	echo(message, err = true)
}

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
		error("Unable to parse repository name from origin URL.")
		throw Abort()
	}

	if (!hasRepoAccess) {
		error("Personal token does not have access to $repoName.")
		throw Abort()
	}
}

context(CliktCommand)
internal fun StackBranch.submit(
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

internal fun StackBranch.flattenUp(): List<StackBranch> {
	return buildList {
		fun StackBranch.addChildren() {
			children.forEach {
				add(it)
				it.addChildren()
			}
		}

		add(this@flattenUp)
		addChildren()
	}
}

internal class PrettyBranch(
	val branch: StackBranch,
	val pretty: String,
)

internal fun StackBranch.prettyTree(
	selected: StackBranch? = null,
	filter: (StackBranch) -> Boolean = { true },
): List<PrettyBranch> {
	return if (!filter(this)) {
		emptyList()
	} else {
		buildList {
			prettyTree(
				builder = this,
				inset = 0,
				treeWidth = treeWidth(filter),
				selected = selected,
				filter = filter,
			)
		}
	}
}

context(MutableList<PrettyBranch>)
internal fun StackBranch.prettyTree(
	builder: MutableList<PrettyBranch>,
	inset: Int,
	treeWidth: Int,
	selected: StackBranch? = null,
	filter: (StackBranch) -> Boolean,
) {
	val filteredChildren = children.filter(filter)
	filteredChildren.forEachIndexed { index, child ->
		child.prettyTree(builder, inset + index, treeWidth, selected, filter)
	}

	val pretty = buildString {
		repeat(inset) { append("│ ") }

		if (this@prettyTree.name == selected?.name) {
			append("◉")
		} else {
			append("○")
		}

		val horizontalBranches = (filteredChildren.size - 1).coerceAtLeast(0)
		if (horizontalBranches > 0) {
			repeat(horizontalBranches - 1) { append("─┴") }
			append("─┘")
		}

		repeat(treeWidth - inset - horizontalBranches - 1) { append("  ") }

		append(" ")
		append(name)
	}

	builder += PrettyBranch(
		branch = this,
		pretty = pretty,
	)
}

internal fun StackBranch.treeWidth(
	filter: ((StackBranch) -> Boolean),
): Int {
	return if (!filter(this)) {
		0
	} else {
		children.sumOf { it.treeWidth(filter) }.coerceAtLeast(1)
	}
}

context(CliktCommand, Locker.LockScope)
internal fun Locker.Operation.Restack.perform(
	stackManager: StackManager,
	vc: VersionControl,
	continuing: Boolean = false,
) {
	branches.forEachIndexed { index, branchName ->
		val branch = stackManager.getBranch(branchName)!!
		if (!continuing || index > 0) {
			if (!vc.restack(branchName = branch.name, parentName = branch.parent!!.name, parentSha = branch.parentSha!!)) {
				error(
					"Merge conflict. Resolve all conflicts manually and then run ${"st rebase --continue".styleCode()}. " +
						"To abort, run ${"st rebase --abort".styleCode()}",
				)
				throw Abort()
			}
		}

		stackManager.updateParentSha(branch, vc.getSha(branch.parent!!.name))
		updateOperation(copy(branches = branches.subList(index + 1, branches.size)))
	}

	vc.checkout(startingBranch)
}

fun main(args: Array<String>) {
	Arena.openConfined().use { arena ->
		val shell = RealShell()
		GitVersionControl(arena, shell).use { vc ->

			val dbPath = vc.configDirectory / "stacker.db"
			withDatabase(dbPath.toString()) { db ->
				val stackManager = RealStackManager(db)
				val configManager = RealConfigManager(db, stackManager)
				val locker = RealLocker(db, stackManager, vc)
				val remote = GitHubRemote(vc.originUrl, configManager)

				Stacker(
					configManager = configManager,
					locker = locker,
					remote = remote,
					stackManager = stackManager,
					vc = vc,
				).main(args)
			}
		}
	}
}
