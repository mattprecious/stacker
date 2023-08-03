package com.mattprecious.stacker.vc

import com.mattprecious.stacker.shell.Shell
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

	override val currentBranch: Branch by lazy {
		val name = shell.exec(COMMAND, "branch", "--show-current")
		Branch(vc = this, name = name)
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

	override fun createBranchFromCurrent(branchName: String) {
		val parent = currentBranch
		shell.exec(COMMAND, "checkout", "-b", branchName)
		setMetadata(branchName, BranchData(isTrunk = false, parentName = parent.name))
	}

	private fun refPath(branchName: String): String {
		return "refs/stacker/branch/$branchName"
	}

	companion object {
		private const val COMMAND = "git"
	}
}
