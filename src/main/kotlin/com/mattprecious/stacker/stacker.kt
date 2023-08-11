package com.mattprecious.stacker

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.config.RealConfigManager
import com.mattprecious.stacker.remote.GitHubRemote
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.rendering.interactivePrompt
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.shell.RealShell
import com.mattprecious.stacker.stack.RealStackManager
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.GitVersionControl
import com.mattprecious.stacker.vc.VersionControl
import kotlin.io.path.div
import com.mattprecious.stacker.stack.Branch as StackBranch

class Stacker(
	private val configManager: ConfigManager,
	remote: Remote,
	stackManager: StackManager,
	vc: VersionControl,
) : CliktCommand(
	name = "st",
) {
	init {
		subcommands(
			Init(
				configManager = configManager,
				vc = vc,
			),
			Log(
				stackManager = stackManager,
				vc = vc,
			),
			Branch(
				vc = vc,
				configManager = configManager,
				remote = remote,
				stackManager = stackManager,
			),
			Stack(
				configManager = configManager,
				remote = remote,
				stackManager = stackManager,
				vc = vc,
			),
		)
	}

	override fun run() {
		if (!configManager.repoInitialized && currentContext.invokedSubcommand !is Init) {
			error(message = "Stacker must be initialized, first. Please run ${"st init".styleCode()}.")
			throw Abort()
		}
	}
}

class Init(
	private val configManager: ConfigManager,
	private val vc: VersionControl,
) : CliktCommand() {
	override fun run() {
		val branches = vc.branches
		val trunk = interactivePrompt(
			message = "Select your trunk branch, which you open pull requests against",
			options = branches,
			// TODO: Infer without hard coding.
			default = configManager.trunk ?: "main",
		)

		val useTrailing = YesNoPrompt(
			terminal = currentContext.terminal,
			prompt = "Do you use a trailing-trunk workflow?",
			default = configManager.trailingTrunk != null,
		).ask() == true

		val trailingTrunk = if (!useTrailing) {
			null
		} else {
			interactivePrompt(
				message = "Select your trailing trunk branch, which you branch from",
				options = branches.filterNot { it == trunk },
				default = configManager.trailingTrunk,
			)
		}

		configManager.initializeRepo(trunk = trunk, trailingTrunk = trailingTrunk)
	}
}

private class Branch(
	configManager: ConfigManager,
	remote: Remote,
	stackManager: StackManager,
	vc: VersionControl,
) : CliktCommand() {
	init {
		subcommands(
			Track(configManager, stackManager, vc),
			Untrack(stackManager, vc),
			Create(stackManager, vc),
			Rename(stackManager, vc),
			Submit(configManager, remote, stackManager, vc),
		)
	}

	override fun run() = Unit

	private class Track(
		private val configManager: ConfigManager,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : CliktCommand() {
		override fun run() {
			val currentBranchName = vc.currentBranchName
			val currentBranch = stackManager.getBranch(currentBranchName)
			if (currentBranch != null) {
				error(message = "Branch ${currentBranch.name.styleBranch()} is already tracked.")
				return
			}

			val defaultName = configManager.trailingTrunk ?: configManager.trunk

			val options = stackManager.getBase()!!.prettyTree { vc.isAncestor(currentBranchName, it) }
			val parent = interactivePrompt(
				message = "Select the parent branch for ${currentBranchName.styleBranch()}",
				options = options,
				default = options.find { it.branch.name == defaultName },
				displayTransform = { it.pretty },
				valueTransform = { it.branch.name },
			)

			stackManager.trackBranch(currentBranchName, parent.branch.name)
		}
	}

	private class Untrack(
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : CliktCommand() {
		override fun run() {
			val currentBranch = stackManager.getBranch(vc.currentBranchName)
			if (currentBranch == null) {
				error(message = "Branch ${vc.currentBranchName.styleBranch()} is already not tracked.")
				return
			}

			stackManager.untrackBranch(currentBranch)
		}
	}

	private class Create(
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : CliktCommand() {
		private val branchName by argument()

		override fun run() {
			val currentBranch = stackManager.getBranch(vc.currentBranchName)
			if (currentBranch == null) {
				error(
					message = "Cannot branch from ${vc.currentBranchName.styleBranch()} since it is not tracked. " +
						"Please track with ${"st branch track".styleCode()}.",
				)
				throw Abort()
			}

			vc.createBranchFromCurrent(branchName)
		}
	}

	private class Rename(
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : CliktCommand() {
		private val newName by argument()

		override fun run() {
			val currentBranchName = vc.currentBranchName
			val currentBranch = stackManager.getBranch(currentBranchName)
			if (currentBranch == null) {
				error(
					message = "Cannot rename ${currentBranchName.styleBranch()} since it is not tracked. " +
						"Please track with ${"st branch track".styleCode()}.",
				)
				throw Abort()
			}

			vc.renameBranch(currentBranch, newName)
			stackManager.renameBranch(currentBranch, newName)
		}
	}

	private class Submit(
		private val configManager: ConfigManager,
		private val remote: Remote,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : CliktCommand() {
		override fun run() {
			val currentBranch = stackManager.getBranch(vc.currentBranchName)
			if (currentBranch == null) {
				error(
					message = "Cannot create a pull request from ${vc.currentBranchName.styleBranch()} since it is " +
						"not tracked. Please track with ${"st branch track".styleCode()}.",
				)
				throw Abort()
			}

			if (currentBranch.name == configManager.trunk || currentBranch.name == configManager.trailingTrunk) {
				error(
					message = "Cannot create a pull request from trunk branch ${currentBranch.name.styleBranch()}.",
				)
				throw Abort()
			}

			remote.requireAuthenticated()

			vc.pushBranches(listOf(currentBranch))
			currentBranch.submit(configManager, remote, vc)
		}
	}
}

private class Stack(
	configManager: ConfigManager,
	remote: Remote,
	stackManager: StackManager,
	vc: VersionControl,
) : CliktCommand() {
	init {
		subcommands(
			Submit(configManager, remote, stackManager, vc),
		)
	}

	override fun run() = Unit

	private class Submit(
		private val configManager: ConfigManager,
		private val remote: Remote,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : CliktCommand() {
		override fun run() {
			val currentBranch = stackManager.getBranch(vc.currentBranchName)
			if (currentBranch == null) {
				error(
					message = "Cannot create a pull request from ${vc.currentBranchName.styleBranch()} since it is " +
						"not tracked. Please track with ${"st branch track".styleCode()}.",
				)
				throw Abort()
			}

			if (currentBranch.name == configManager.trunk || currentBranch.name == configManager.trailingTrunk) {
				error(
					message = "Cannot create a pull request from trunk branch ${currentBranch.name.styleBranch()}.",
				)
				throw Abort()
			}

			remote.requireAuthenticated()

			val branchesToSubmit = currentBranch.flattenStack()
				.filterNot { it.name == configManager.trunk || it.name == configManager.trailingTrunk }
			vc.pushBranches(branchesToSubmit)
			branchesToSubmit.forEach { it.submit(configManager, remote, vc) }
		}
	}
}

private class Log(
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : CliktCommand() {
	override fun run() {
		echo(
			stackManager.getBase()?.prettyTree()?.joinToString("\n") {
				if (vc.needsRestack(it.branch)) {
					"${it.pretty} (needs restack)"
				} else {
					it.pretty
				}
			},
		)
	}
}

context(CliktCommand)
private fun error(message: String) {
	echo(message, err = true)
}

context(CliktCommand)
private fun Remote.requireAuthenticated() {
	if (!isAuthenticated) {
		prompt(
			text = "Please enter a GitHub access token",
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
private fun StackBranch.submit(
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
		val info = vc.latestCommitInfo(this)
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

private fun StackBranch.flattenStack(): List<StackBranch> {
	return buildList {
		fun StackBranch.addParents() {
			if (parent != null) {
				parent!!.addParents()
				add(parent!!)
			}
		}

		fun StackBranch.addChildren() {
			children.forEach {
				add(it)
				it.addChildren()
			}
		}

		addParents()
		add(this@flattenStack)
		addChildren()
	}
}

private class PrettyBranch(
	val branch: StackBranch,
	val pretty: String,
)

private fun StackBranch.prettyTree(
	filter: (StackBranch) -> Boolean = { true },
): List<PrettyBranch> {
	return if (!filter(this)) {
		emptyList()
	} else {
		buildList { prettyTree(this, inset = 0, treeWidth = treeWidth(filter), filter = filter) }
	}
}

context(MutableList<PrettyBranch>)
private fun StackBranch.prettyTree(
	builder: MutableList<PrettyBranch>,
	inset: Int,
	treeWidth: Int,
	filter: (StackBranch) -> Boolean,
) {
	val filteredChildren = children.filter(filter)
	filteredChildren.forEachIndexed { index, child ->
		child.prettyTree(builder, inset + index, treeWidth, filter)
	}

	val pretty = buildString {
		repeat(inset) { append("│ ") }
		append("○")

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

private fun StackBranch.treeWidth(
	filter: ((StackBranch) -> Boolean),
): Int {
	return if (!filter(this)) {
		0
	} else {
		children.sumOf { it.treeWidth(filter) }.coerceAtLeast(1)
	}
}

fun main(args: Array<String>) {
	val shell = RealShell()
	val vc = GitVersionControl(shell)

	val dbPath = vc.configDirectory / "stacker.db"
	withDatabase(dbPath.toString()) { db ->
		val stackManager = RealStackManager(db)
		val configManager = RealConfigManager(db, stackManager)
		val remote = GitHubRemote(vc.originUrl, configManager)

		Stacker(
			configManager = configManager,
			remote = remote,
			stackManager = stackManager,
			vc = vc,
		).main(args)
	}
}
