package com.mattprecious.stacker.vc

import com.mattprecious.stacker.stack.Branch
import java.nio.file.Path

interface VersionControl {
	val root: Path
	val configDirectory: Path
	val currentBranchName: String
	val originUrl: String

	fun fallthrough(commands: List<String>)

	fun createBranchFromCurrent(branchName: String)

	fun renameBranch(branch: Branch, newName: String)

	fun pushBranches(branches: List<Branch>)

	fun latestCommitInfo(branch: Branch): CommitInfo

	data class CommitInfo(
		val title: String,
		val body: String?,
	)
}
