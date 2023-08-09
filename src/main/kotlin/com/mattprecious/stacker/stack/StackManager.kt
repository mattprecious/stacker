package com.mattprecious.stacker.stack

interface StackManager {
	fun getBase(): Branch?
	fun getBranch(branchName: String): Branch?

	fun trackBranch(
		branchName: String,
		parentName: String?,
	)

	fun untrackBranch(
		branch: Branch,
	)
}
