package com.mattprecious.stacker.vc

import com.mattprecious.stacker.stack.Branch
import java.nio.file.Path

interface VersionControl : AutoCloseable {
	val configDirectory: Path
	val currentBranchName: String
	val originUrl: String
	val branches: List<String>

	fun fallthrough(commands: List<String>)

	fun checkout(branchName: String)

	fun createBranchFromCurrent(branchName: String)

	fun renameBranch(branch: Branch, newName: String)

	fun delete(branchName: String)

	fun pushBranches(branches: List<Branch>)

	fun latestCommitInfo(branch: Branch): CommitInfo

	fun isAncestor(branchName: String, possibleAncestor: Branch): Boolean

	fun needsRestack(branch: Branch): Boolean

	fun restack(branch: Branch): Boolean

	fun getSha(branch: String): String

	fun abortRebase()

	fun continueRebase(branchName: String): Boolean

	data class CommitInfo(
		val title: String,
		val body: String?,
	)
}
