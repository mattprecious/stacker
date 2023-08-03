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

	fun createBranchFromCurrent(branchName: String)
}
