package com.mattprecious.stacker.stack

import com.mattprecious.stacker.collections.TreeNode
import com.mattprecious.stacker.db.Branch

interface StackManager {
	val trackedBranchNames: List<String>

	fun getBase(): TreeNode<Branch>?
	fun getBranch(branchName: String): TreeNode<Branch>?

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
	) = updateParent(branch.name, parent.name)

	fun updateParent(
		branch: String,
		parent: String,
	)

	fun updateParentSha(
		branch: Branch,
		parentSha: String,
	)

	fun updatePrNumber(
		branch: Branch,
		prNumber: Long,
	)

	fun setHasAskedToDelete(branch: Branch)
}
