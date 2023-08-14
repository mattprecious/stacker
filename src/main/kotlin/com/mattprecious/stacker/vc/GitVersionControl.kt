package com.mattprecious.stacker.vc

import com.mattprecious.stacker.shell.Shell
import com.mattprecious.stacker.stack.Branch
import com.mattprecious.stacker.vc.VersionControl.BranchInfo
import com.mattprecious.stacker.vc.VersionControl.CommitInfo
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.exists

class GitVersionControl(
	private val shell: Shell,
) : VersionControl {
	override val configDirectory: Path
		get() = Path.of(shell.exec(COMMAND, "rev-parse", "--git-dir"))

	override val currentBranchName: String
		get() {
			return shell.exec(COMMAND, "branch", "--show-current")
		}

	override val originUrl: String by lazy {
		shell.exec(COMMAND, "remote", "get-url", "origin")
	}

	override val branches: List<String>
		get() = shell.exec(COMMAND, "branch", "--format=%(refname:short)").split('\n')

	override fun fallthrough(commands: List<String>) {
		shell.exec(COMMAND, *commands.toTypedArray())
	}

	override fun createBranchFromCurrent(branchName: String) {
		shell.exec(COMMAND, "checkout", "-b", branchName)
	}

	override fun renameBranch(branch: Branch, newName: String) {
		shell.exec(COMMAND, "branch", "-m", branch.name, newName)
	}

	override fun pushBranches(branches: List<Branch>) {
		shell.exec(COMMAND, "push", "-f", "--atomic", "origin", *branches.map { it.name }.toTypedArray())
	}

	override fun latestCommitInfo(branch: Branch): CommitInfo {
		val title = shell.exec(COMMAND, "log", "-1", "--format=format:%s", branch.name).trim()
		val body = shell.exec(COMMAND, "log", "-1", "--format=format:%b", branch.name).ifBlank { null }

		return CommitInfo(
			title = title,
			body = body,
		)
	}

	override fun isAncestor(branchName: String, possibleAncestor: Branch): Boolean {
		return shell.execStatus(COMMAND, "merge-base", "--is-ancestor", possibleAncestor.name, branchName)
	}

	override fun needsRestack(branch: Branch): Boolean {
		val parent = branch.parent ?: return false
		return needsRestack(parent) || !isAncestor(branch.name, parent)
	}

	override fun restack(branch: Branch, newParent: Branch?, beforeRebase: (Branch) -> Unit) {
		val startingBranch = currentBranchName
		performRestack(branch, newParent, beforeRebase)

		if (startingBranch != currentBranchName) {
			shell.exec(COMMAND, "checkout", startingBranch)
		}
	}

	override fun getShas(branches: List<String>): List<BranchInfo> {
		return branches.map {
			BranchInfo(
				name = it,
				sha = shell.exec(COMMAND, "rev-parse", it),
			)
		}
	}

	override fun reset(branches: List<BranchInfo>) {
		// So incredibly gross...
		val rebaseInProgress = (configDirectory / "rebase_in_progress").exists() ||
			(configDirectory / "interactive_rebase_in_progress").exists()
		if (rebaseInProgress) {
			shell.exec(COMMAND, "rebase", "--abort")
		}

		val current = currentBranchName
		branches.forEach {
			if (it.name == current) {
				shell.exec(COMMAND, "reset", "--hard", it.sha)
			} else {
				shell.exec(COMMAND, "branch", "-f", it.name, it.sha)
			}
		}
	}

	private fun performRestack(
		branch: Branch,
		newParent: Branch?,
		beforeRebase: (Branch) -> Unit,
	) {
		val currentParent = branch.parent!!
		val rebaseOnto = newParent ?: currentParent

		// I have absolutely no idea if these commands are sufficient. It seems too easy. But let's try it out.
		val forkPoint = shell.exec(COMMAND, "merge-base", "--fork-point", currentParent.name, branch.name)

		beforeRebase(branch)

		// TODO: Detect that a merge conflict has occured and intercept the error message, replacing it with one that
		//  suggests st commands instead of git rebase commands.
		shell.exec(COMMAND, "rebase", "--onto", rebaseOnto.name, forkPoint, branch.name)

		branch.children.forEach { performRestack(it, null, beforeRebase) }
	}

	companion object {
		private const val COMMAND = "git"
	}
}
