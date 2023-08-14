package com.mattprecious.stacker

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.config.RealConfigManager
import com.mattprecious.stacker.lock.BranchState
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.lock.RealLocker
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
	private val locker: Locker,
	remote: Remote,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : CliktCommand(
	name = "st",
	invokeWithoutSubcommand = true,
) {
	private val abort: Boolean by option().flag()
	private val cont by option("--continue").flag()

	init {
		subcommands(
			Branch(
				vc = vc,
				configManager = configManager,
				remote = remote,
				stackManager = stackManager,
			),
			Init(
				configManager = configManager,
				vc = vc,
			),
			Log(
				stackManager = stackManager,
				vc = vc,
			),
			Stack(
				configManager = configManager,
				remote = remote,
				stackManager = stackManager,
				vc = vc,
			),
			Upstack(
				configManager = configManager,
				locker = locker,
				stackManager = stackManager,
				vc = vc,
			),
		)
	}

	override fun run() {
		if (currentContext.invokedSubcommand == null) {
			when {
				abort -> abortOperation()
				cont -> continueOperation()
				else -> throw PrintHelpMessage(currentContext, error = true)
			}
		}

		if (!configManager.repoInitialized && currentContext.invokedSubcommand !is Init) {
			error(message = "Stacker must be initialized, first. Please run ${"st init".styleCode()}.")
			throw Abort()
		}

		if (locker.hasLock()) {
			error(
				"A restack is currently in progress. Please run ${"st --abort".styleCode()} or resolve any " +
					"conflicts and run ${"st --continue".styleCode()}.",
			)
			throw Abort()
		}
	}

	private fun abortOperation() {
		if (!locker.hasLock()) {
			error("Nothing to abort.")
			throw Abort()
		}

		vc.reset(locker.getLockedBranches().map { it.toInfo() })
		locker.cancelOperation()
	}

	private fun continueOperation() {
		if (!locker.hasLock()) {
			error("Nothing to continue.")
			throw Abort()
		}

		// TODO: This is a complete re-implementation of the beginOperation block. These should share code.
		locker.continueOperation { operation ->
			when (operation) {
				is Locker.Operation.Restack -> {
					val branch = stackManager.getBranch(operation.branchName)!!
					val newParent = stackManager.getBranch(operation.ontoName)!!
					val nextBranch = stackManager.getBranch(operation.nextBranchToRebase)!!

					val parentForRestack = if (nextBranch == branch) newParent else null
					vc.restack(nextBranch, parentForRestack) {
						updateOperation(operation.copy(nextBranchToRebase = it.name))
					}

					stackManager.updateParent(branch, newParent)
				}
			}
		}
	}

	private fun BranchState.toInfo() = VersionControl.BranchInfo(
		name = name,
		sha = sha,
	)
}

class Init(
	private val configManager: ConfigManager,
	private val vc: VersionControl,
) : CliktCommand() {
	override fun run() {
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
			Bottom(configManager, stackManager, vc),
			Checkout(stackManager, vc),
			Create(stackManager, vc),
			Down(stackManager, vc),
			Rename(stackManager, vc),
			Submit(configManager, remote, stackManager, vc),
			Top(stackManager, vc),
			Track(configManager, stackManager, vc),
			Untrack(stackManager, vc),
			Up(stackManager, vc),
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

	private class Checkout(
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : CliktCommand() {
		override fun run() {
			val options = stackManager.getBase()!!.prettyTree()
			val branch = interactivePrompt(
				message = "Checkout a branch",
				options = options,
				default = options.find { it.branch.name == vc.currentBranchName },
				displayTransform = { it.pretty },
				valueTransform = { it.branch.name },
			)

			vc.checkout(branch.branch)
		}
	}

	private class Up(
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : CliktCommand() {
		override fun run() {
			val options = stackManager.getBranch(vc.currentBranchName)!!.children
			val branch = interactivePrompt(
				message = "Move up to",
				options = options,
				displayTransform = { it.name },
				valueTransform = { it.name },
			)

			vc.checkout(branch)
		}
	}

	private class Down(
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : CliktCommand() {
		override fun run() {
			val parent = stackManager.getBranch(vc.currentBranchName)!!.parent
			if (parent == null) {
				error("Already at the base.")
				throw Abort()
			}

			vc.checkout(parent)
		}
	}

	private class Top(
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : CliktCommand() {
		override fun run() {
			val options = stackManager.getBranch(vc.currentBranchName)!!.leaves()
			val branch = interactivePrompt(
				message = "Choose which top",
				options = options,
				displayTransform = { it.name },
				valueTransform = { it.name },
			)

			vc.checkout(branch)
		}
	}

	private class Bottom(
		private val configManager: ConfigManager,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : CliktCommand() {
		override fun run() {
			val trunk = configManager.trunk
			val trailingTrunk = configManager.trailingTrunk
			val currentBranch = stackManager.getBranch(vc.currentBranchName)!!

			if (currentBranch.name == trailingTrunk || currentBranch.name == trunk) {
				error("Not in a stack.")
				throw Abort()
			}

			var bottom = currentBranch
			while (bottom.parent!!.name != trailingTrunk) {
				bottom = bottom.parent!!
			}

			vc.checkout(bottom)
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

private class Upstack(
	configManager: ConfigManager,
	locker: Locker,
	stackManager: StackManager,
	vc: VersionControl,
) : CliktCommand() {
	init {
		subcommands(
			Onto(configManager, locker, stackManager, vc),
		)
	}

	override fun run() = Unit

	private class Onto(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : CliktCommand() {
		override fun run() {
			val currentBranchName = vc.currentBranchName
			val currentBranch = stackManager.getBranch(currentBranchName)
			if (currentBranch == null) {
				error(
					message = "Cannot retarget ${currentBranchName.styleBranch()} since it is not tracked. " +
						"Please track with ${"st branch track".styleCode()}.",
				)
				throw Abort()
			}

			if (currentBranchName == configManager.trunk || currentBranchName == configManager.trailingTrunk) {
				error(message = "Cannot retarget a trunk branch.")
				throw Abort()
			}

			val options = stackManager.getBase()!!.prettyTree { it.name != currentBranchName }
			val newParent = interactivePrompt(
				message = "Select the parent branch for ${currentBranchName.styleBranch()}",
				options = options,
				default = options.find { it.branch.name == currentBranch.parent!!.name },
				displayTransform = { it.pretty },
				valueTransform = { it.branch.name },
			).branch

			val operation = Locker.Operation.Restack(
				branchName = currentBranchName,
				ontoName = newParent.name,
				nextBranchToRebase = currentBranchName,
			)

			locker.beginOperation(operation) {
				vc.restack(currentBranch, newParent) {
					updateOperation(operation.copy(nextBranchToRebase = it.name))
				}

				stackManager.updateParent(currentBranch, newParent)
			}
		}
	}
}

private class Log(
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : CliktCommand() {
	override fun run() {
		echo(
			stackManager.getBase()?.prettyTree(
				selected = stackManager.getBranch(vc.currentBranchName),
			)?.joinToString("\n") {
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
private fun StackBranch.prettyTree(
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

private fun StackBranch.treeWidth(
	filter: ((StackBranch) -> Boolean),
): Int {
	return if (!filter(this)) {
		0
	} else {
		children.sumOf { it.treeWidth(filter) }.coerceAtLeast(1)
	}
}

private fun StackBranch.leaves(): List<StackBranch> {
	return if (children.isEmpty()) {
		listOf(this)
	} else {
		children.flatMap { it.leaves() }
	}
}

fun main(args: Array<String>) {
	val shell = RealShell()
	val vc = GitVersionControl(shell)

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
