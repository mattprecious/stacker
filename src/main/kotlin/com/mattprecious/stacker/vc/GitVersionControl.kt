package com.mattprecious.stacker.vc

import com.mattprecious.stacker.shell.Shell
import com.mattprecious.stacker.stack.Branch
import com.mattprecious.stacker.vc.VersionControl.CommitInfo
import java.nio.file.Path
import kotlin.io.path.div

class GitVersionControl(
	private val shell: Shell,
) : VersionControl {
	override val root: Path by lazy {
		Path.of(shell.exec(COMMAND, "rev-parse", "--show-toplevel"))
	}

	override val configDirectory: Path
		get() = root / ".git"

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

	companion object {
		private const val COMMAND = "git"
	}
}
