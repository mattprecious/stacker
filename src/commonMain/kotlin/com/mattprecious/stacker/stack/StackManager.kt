package com.mattprecious.stacker.stack

interface StackManager {
	val trackedBranchNames: List<String>

	fun getBase(): Branch?
	fun getBranch(branchName: String): Branch?

	fun trackBranch(
		branchName: String,
		parentName: String?,
		parentSha: String?,
	)

	fun untrackBranch(
		branch: Branch,
	)

	fun untrackBranches(
		branches: Set<String>,
	)

	fun renameBranch(
		branch: Branch,
		newName: String,
	)

	fun updateParent(
		branch: Branch,
		parent: Branch,
	)

	fun updateParentSha(
		branch: Branch,
		parentSha: String,
	)
}
