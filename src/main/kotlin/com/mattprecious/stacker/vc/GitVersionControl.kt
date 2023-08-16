package com.mattprecious.stacker.vc

import com.mattprecious.stacker.shell.Shell
import com.mattprecious.stacker.stack.Branch
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

	override fun checkout(branch: Branch) {
		shell.exec(COMMAND, "checkout", branch.name)
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
		val parentSha = getSha(parent.name)
		return branch.parentSha != parentSha || !isAncestor(branch.name, parent)
	}

	override fun restack(branch: Branch) {
		// TODO: Detect that a merge conflict has occured and intercept the error message, replacing it with one that
		//  suggests st commands instead of git rebase commands.
		// I have absolutely no idea if this command is sufficient. It seems too easy. But let's try it out.
		shell.exec(COMMAND, "rebase", "--onto", branch.parent!!.name, branch.parentSha!!, branch.name)
	}

	override fun getSha(branch: String): String {
		return shell.exec(COMMAND, "rev-parse", branch)
	}

	override fun abortRebase() {
		if (rebaseInProgress()) {
			shell.exec(COMMAND, "-c", "core.editor=true", "rebase", "--abort")
		}
	}

	override fun continueRebase() {
		if (rebaseInProgress()) {
			shell.exec(COMMAND, "-c", "core.editor=true", "rebase", "--continue")
		}
	}

	private fun rebaseInProgress() = (configDirectory / "rebase-merge").exists()

	companion object {
		private const val COMMAND = "git"
	}
}
