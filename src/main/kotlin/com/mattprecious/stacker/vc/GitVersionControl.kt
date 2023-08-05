package com.mattprecious.stacker.vc

import com.mattprecious.stacker.shell.Shell
import com.mattprecious.stacker.vc.VersionControl.CommitInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Buffer
import java.nio.file.Path
import kotlin.io.path.div

class GitVersionControl(
	private val shell: Shell,
) : VersionControl {
	override val root: Path by lazy {
		Path.of(shell.exec(COMMAND, "rev-parse", "--show-toplevel"))
	}

	override val configDirectory: Path
		get() = root / ".git/stacker"

	override val currentBranch: Branch
		get() {
			val name = shell.exec(COMMAND, "branch", "--show-current")
			return Branch(vc = this, name = name)
		}

	override val originUrl: String by lazy {
		shell.exec(COMMAND, "remote", "get-url", "origin")
	}

	override fun fallthrough(commands: List<String>) {
		shell.exec(COMMAND, *commands.toTypedArray())
	}

	override fun setMetadata(
		branchName: String,
		data: BranchData?,
	) {
		if (data == null) {
			shell.exec(COMMAND, "update-ref", "-d", refPath(branchName))
			return
		}

		val json = Json.encodeToString(data)
		val objectHash = shell.exec(
			COMMAND,
			"hash-object",
			"-w",
			"--stdin",
			input = Buffer().writeUtf8(json),
		)
		shell.exec(COMMAND, "update-ref", refPath(branchName), objectHash)
	}

	override fun getMetadata(branchName: String): BranchData? {
		val json = shell.exec(COMMAND, "show", refPath(branchName), suppressErrors = true)
		return if (json.isNotEmpty()) {
			Json.decodeFromString(json)
		} else {
			null
		}
	}

	override fun getBranch(branchName: String): Branch {
		return Branch(this, branchName)
	}

	override fun track(
		branch: Branch,
		isTrunk: Boolean,
	) {
		setMetadata(branch.name, BranchData(isTrunk = isTrunk, parentName = null, children = emptyList()))
	}

	override fun untrack(branch: Branch) {
		setMetadata(branch.name, null)
	}

	override fun createBranchFromCurrent(branchName: String) {
		val parent = currentBranch

		shell.exec(COMMAND, "checkout", "-b", branchName)
		currentBranch.track(parent)
	}

	override fun pushCurrentBranch() {
		require(!currentBranch.isTrunk) {
			"Will not push trunk branch: ${currentBranch.name}."
		}

		shell.exec(COMMAND, "push", "-f", "origin", currentBranch.name)
	}

	override fun pushBranches(branches: List<Branch>) {
		require(branches.none { it.isTrunk }) {
			"Will not push trunk branch: ${branches.first { it.isTrunk }}."
		}

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

	private fun refPath(branchName: String): String {
		return "refs/stacker/branch/$branchName"
	}

	companion object {
		private const val COMMAND = "git"
	}
}
