package com.mattprecious.stacker.vc

import java.nio.file.Path

interface VersionControl {
	val root: Path
	val configDirectory: Path
	val currentBranch: Branch
	val originUrl: String

	fun fallthrough(commands: List<String>)

	fun setMetadata(
		branchName: String,
		data: BranchData?,
	)

	fun getMetadata(branchName: String): BranchData?

	fun getBranch(branchName: String): Branch

	fun track(branch: Branch, isTrunk: Boolean)

	fun untrack(branch: Branch)

	fun createBranchFromCurrent(branchName: String)

	fun pushCurrentBranch()

	fun pushBranches(branches: List<Branch>)

	fun latestCommitInfo(branch: Branch = currentBranch): CommitInfo

	data class CommitInfo(
		val title: String,
		val body: String?,
	)
}
