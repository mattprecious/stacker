package com.mattprecious.stacker

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.config.RealConfigManager
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
import java.lang.foreign.Arena
import kotlin.io.path.div
import com.mattprecious.stacker.stack.Branch as StackBranch

private class Stacker(
	configManager: ConfigManager,
	locker: Locker,
	remote: Remote,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(
	name = "st",
) {
	init {
		subcommands(
			Branch(
				configManager = configManager,
				locker = locker,
				remote = remote,
				stackManager = stackManager,
				vc = vc,
			),
			Log(
				configManager = configManager,
				stackManager = stackManager,
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
				vc = vc,
			),
		)
	}

	override fun run() {
		stackManager.reconcileBranches(vc)
	}
}

private class Repo(
	configManager: ConfigManager,
	locker: Locker,
	vc: VersionControl,
) : StackerCommand(shortAlias = "r") {
	init {
		subcommands(
			Init(configManager, locker, vc),
		)
	}

	override fun run() = Unit

	private class Init(
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
}

private class Branch(
	configManager: ConfigManager,
	locker: Locker,
	remote: Remote,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCommand(shortAlias = "b") {
	init {
		subcommands(
			Bottom(configManager, locker, stackManager, vc),
			Checkout(configManager, locker, stackManager, vc),
			Create(configManager, locker, stackManager, vc),
			Delete(configManager, locker, stackManager, vc),
			Down(configManager, locker, stackManager, vc),
			Rename(configManager, locker, stackManager, vc),
			Restack(configManager, locker, stackManager, vc),
			Submit(configManager, locker, remote, stackManager, vc),
			Top(configManager, locker, stackManager, vc),
			Track(configManager, locker, stackManager, vc),
			Untrack(configManager, locker, stackManager, vc),
			Up(configManager, locker, stackManager, vc),
		)
	}

	override fun run() = Unit

	private class Track(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "tr") {
		private val branchName: String? by argument().optional()

		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

			val branchName = branchName ?: vc.currentBranchName
			val currentBranch = stackManager.getBranch(branchName)
			if (currentBranch != null) {
				error(message = "Branch ${branchName.styleBranch()} is already tracked.")
				return
			}

			val trunk = configManager.trunk
			val trailingTrunk = configManager.trailingTrunk

			val defaultName = trailingTrunk ?: trunk

			val options = stackManager.getBase()!!.prettyTree {
				it.name == trunk || it.name == trailingTrunk || vc.isAncestor(
					branchName = branchName,
					possibleAncestorName = it.name,
				)
			}
			val parent = interactivePrompt(
				message = "Select the parent branch for ${branchName.styleBranch()}",
				options = options,
				default = options.find { it.branch.name == defaultName },
				displayTransform = { it.pretty },
				valueTransform = { it.branch.name },
			).branch.name

			val parentSha = vc.getSha(parent)

			stackManager.trackBranch(branchName, parent, parentSha)
		}
	}

	private class Untrack(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "ut") {
		private val branchName: String? by argument().optional()

		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

			val branchName = branchName ?: vc.currentBranchName
			val currentBranch = stackManager.getBranch(branchName)
			if (currentBranch == null) {
				error(message = "Branch ${branchName.styleBranch()} is already not tracked.")
				return
			}

			stackManager.untrackBranch(currentBranch)
		}
	}

	private class Create(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "c") {
		private val branchName by argument()

		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

			val currentBranch = stackManager.getBranch(vc.currentBranchName)
			if (currentBranch == null) {
				error(
					message = "Cannot branch from ${vc.currentBranchName.styleBranch()} since it is not tracked. " +
						"Please track with ${"st branch track".styleCode()}.",
				)
				throw Abort()
			}

			vc.createBranchFromCurrent(branchName)
			stackManager.trackBranch(branchName, currentBranch.name, vc.getSha(currentBranch.name))
		}
	}

	private class Rename(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "rn") {
		private val newName by argument()

		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

			val currentBranchName = vc.currentBranchName
			val currentBranch = stackManager.getBranch(currentBranchName)
			if (currentBranch == null) {
				error(
					message = "Cannot rename ${currentBranchName.styleBranch()} since it is not tracked. " +
						"Please track with ${"st branch track".styleCode()}.",
				)
				throw Abort()
			}

			vc.renameBranch(branchName = currentBranch.name, newName = newName)
			stackManager.renameBranch(currentBranch, newName)
		}
	}

	private class Delete(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "dl") {
		private val branchName: String? by argument().optional()

		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

			val currentBranchName = vc.currentBranchName

			val branchName = branchName ?: currentBranchName
			if (branchName == configManager.trunk || branchName == configManager.trailingTrunk) {
				error("Cannnot delete a trunk branch.")
				throw Abort()
			}

			val branch = stackManager.getBranch(branchName)
			if (branch != null) {
				if (branch.children.isNotEmpty()) {
					error("Branch has children. Please retarget or untrack them.")
					throw Abort()
				}

				if (branchName == currentBranchName) {
					vc.checkout(branch.parent!!.name)
				}

				stackManager.untrackBranch(branch)
			}

			vc.delete(branchName)
		}
	}

	private class Checkout(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "co") {
		private val branchName: String? by argument().optional()

		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

			val branch = if (branchName == null) {
				val options = stackManager.getBase()!!.prettyTree()
				interactivePrompt(
					message = "Checkout a branch",
					options = options,
					default = options.find { it.branch.name == vc.currentBranchName },
					displayTransform = { it.pretty },
					valueTransform = { it.branch.name },
				).branch.name
			} else if (vc.branches.contains(branchName)) {
				branchName!!
			} else {
				error("'$branchName' does not match any branches known to git.")
				throw Abort()
			}

			vc.checkout(branch)
		}
	}

	private class Up(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "u") {
		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

			val options = stackManager.getBranch(vc.currentBranchName)!!.children
			val branch = interactivePrompt(
				message = "Move up to",
				options = options,
				displayTransform = { it.name },
				valueTransform = { it.name },
			)

			vc.checkout(branch.name)
		}
	}

	private class Down(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "d") {
		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

			val parent = stackManager.getBranch(vc.currentBranchName)!!.parent
			if (parent == null) {
				error("Already at the base.")
				throw Abort()
			}

			vc.checkout(parent.name)
		}
	}

	private class Top(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "t") {
		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

			val options = stackManager.getBranch(vc.currentBranchName)!!.leaves()
			val branch = interactivePrompt(
				message = "Choose which top",
				options = options,
				displayTransform = { it.name },
				valueTransform = { it.name },
			)

			vc.checkout(branch.name)
		}
	}

	private class Bottom(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "b") {
		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

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

			vc.checkout(bottom.name)
		}
	}

	private class Restack(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "r") {
		private val branchName: String? by argument().optional()

		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

			val currentBranchName = vc.currentBranchName
			val branchName = branchName ?: currentBranchName
			if (stackManager.getBranch(currentBranchName) == null) {
				error(
					message = "Cannot restack ${currentBranchName.styleBranch()} since it is not tracked. " +
						"Please track with ${"st branch track".styleCode()}.",
				)
				throw Abort()
			}

			val operation = Locker.Operation.Restack(
				startingBranch = currentBranchName,
				branches = listOf(branchName),
			)

			locker.beginOperation(operation) {
				operation.perform(stackManager, vc)
			}
		}
	}

	private class Submit(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val remote: Remote,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "s") {
		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

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

			vc.pushBranches(listOf(currentBranch.name))
			currentBranch.submit(configManager, remote, vc)
		}
	}
}

private class Stack(
	configManager: ConfigManager,
	locker: Locker,
	remote: Remote,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCommand(shortAlias = "s") {
	init {
		subcommands(
			Submit(configManager, locker, remote, stackManager, vc),
		)
	}

	override fun run() = Unit

	private class Submit(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val remote: Remote,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "s") {
		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

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
			vc.pushBranches(branchesToSubmit.map { it.name })
			branchesToSubmit.forEach { it.submit(configManager, remote, vc) }
		}
	}
}

private class Upstack(
	configManager: ConfigManager,
	locker: Locker,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCommand(shortAlias = "us") {
	init {
		subcommands(
			Onto(configManager, locker, stackManager, vc),
			Restack(configManager, locker, stackManager, vc),
		)
	}

	override fun run() = Unit

	private class Restack(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "r") {
		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

			val currentBranchName = vc.currentBranchName
			val currentBranch = stackManager.getBranch(currentBranchName)
			if (currentBranch == null) {
				error(
					message = "Cannot restack ${currentBranchName.styleBranch()} since it is not tracked. " +
						"Please track with ${"st branch track".styleCode()}.",
				)
				throw Abort()
			}

			val operation = Locker.Operation.Restack(
				startingBranch = currentBranch.name,
				currentBranch.flattenUp().map { it.name },
			)

			locker.beginOperation(operation) {
				operation.perform(stackManager, vc)
			}
		}
	}

	private class Onto(
		private val configManager: ConfigManager,
		private val locker: Locker,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "o") {
		override fun run() {
			requireInitialized(configManager)
			requireNoLock(locker)

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

			stackManager.updateParent(currentBranch, newParent)

			val operation = Locker.Operation.Restack(
				startingBranch = currentBranch.name,
				currentBranch.flattenUp().map { it.name },
			)

			locker.beginOperation(operation) {
				operation.perform(stackManager, vc)
			}
		}
	}
}

private class Rebase(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	private val abort: Boolean by option().flag()
	private val cont by option("--continue").flag()

	override fun run() {
		requireInitialized(configManager)

		when {
			abort -> abortOperation()
			cont -> continueOperation()
			else -> throw PrintHelpMessage(currentContext, error = true)
		}
	}

	private fun abortOperation() {
		if (!locker.hasLock()) {
			error("Nothing to abort.")
			throw Abort()
		}

		locker.cancelOperation { operation ->
			when (operation) {
				is Locker.Operation.Restack -> {
					vc.abortRebase()
					vc.checkout(operation.startingBranch)
				}
			}
		}
	}

	private fun continueOperation() {
		if (!locker.hasLock()) {
			error("Nothing to continue.")
			throw Abort()
		}

		locker.continueOperation { operation ->
			when (operation) {
				is Locker.Operation.Restack -> {
					if (vc.continueRebase(operation.branches.first())) {
						operation.perform(stackManager, vc, continuing = true)
					}
				}
			}
		}
	}
}

private class Log(
	configManager: ConfigManager,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCommand(shortAlias = "l") {
	init {
		subcommands(
			Short(configManager, stackManager, vc),
		)
	}

	override fun run() = Unit

	private class Short(
		private val configManager: ConfigManager,
		private val stackManager: StackManager,
		private val vc: VersionControl,
	) : StackerCommand(shortAlias = "s") {
		override fun run() {
			requireInitialized(configManager)

			echo(
				stackManager.getBase()?.prettyTree(
					selected = stackManager.getBranch(vc.currentBranchName),
				)?.joinToString("\n") {
					val needsRestack = run needsRestack@{
						val parent = it.branch.parent ?: return@needsRestack false
						val parentSha = vc.getSha(parent.name)
						return@needsRestack it.branch.parentSha != parentSha || !vc.isAncestor(
							branchName = it.branch.name,
							possibleAncestorName = parent.name,
						)
					}

					if (needsRestack) {
						"${it.pretty} (needs restack)"
					} else {
						it.pretty
					}
				},
			)
		}
	}
}

context(CliktCommand)
fun error(message: String) {
	echo(message, err = true)
}

context(CliktCommand)
private fun Remote.requireAuthenticated() {
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

private fun StackManager.reconcileBranches(
	vc: VersionControl,
) {
	untrackBranches(vc.checkBranches(trackedBranchNames.toSet()))
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

private fun StackBranch.flattenUp(): List<StackBranch> {
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

context(CliktCommand, Locker.LockScope)
private fun Locker.Operation.Restack.perform(
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
