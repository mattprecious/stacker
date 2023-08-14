package com.mattprecious.stacker.vc

import com.mattprecious.stacker.stack.Branch
import java.nio.file.Path

interface VersionControl {
	val configDirectory: Path
	val currentBranchName: String
	val originUrl: String
	val branches: List<String>

	fun fallthrough(commands: List<String>)

	fun createBranchFromCurrent(branchName: String)

	fun renameBranch(branch: Branch, newName: String)

	fun pushBranches(branches: List<Branch>)

	fun latestCommitInfo(branch: Branch): CommitInfo

	fun isAncestor(branchName: String, possibleAncestor: Branch): Boolean

	fun needsRestack(branch: Branch): Boolean

	fun restack(branch: Branch, newParent: Branch? = null, beforeRebase: (Branch) -> Unit)

	fun getShas(branches: List<String>): List<BranchInfo>

	fun reset(branches: List<BranchInfo>)

	data class BranchInfo(
		val name: String,
		val sha: String,
	)

	data class CommitInfo(
		val title: String,
		val body: String?,
	)
}
