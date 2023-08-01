package com.mattprecious.stacker.vc

import java.nio.file.Path

interface VersionControl {
	val root: Path
	val configDirectory: Path
	val currentBranch: Branch

	fun fallthrough(commands: List<String>)

	fun setMetadata(
		branchName: String,
		data: BranchData?,
	)

	fun getMetadata(branchName: String): BranchData?
}
