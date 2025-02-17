package com.mattprecious.stacker.stack

import com.mattprecious.stacker.collections.TreeNode
import com.mattprecious.stacker.collections.all
import com.mattprecious.stacker.collections.treeOf
import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.db.RepoDatabase

class RealStackManager(
	db: RepoDatabase,
) : StackManager {
	private val branchQueries = db.branchQueries

	override val trackedBranchNames: List<String>
		get() = branchQueries.names().executeAsList()

	override fun getBase(): TreeNode<Branch>? {
		return getTree()
	}

	override fun getBranch(branchName: String): TreeNode<Branch>? {
		return getTree()?.all?.firstOrNull { it.value.name == branchName }
	}

	override fun trackBranch(branchName: String, parentName: String?, parentSha: String?) {
		branchQueries.insert(
			name = branchName,
			parent = parentName,
			parentSha = parentSha,
			prNumber = null,
		)
	}

	override fun untrackBranch(branch: Branch) {
		untrackBranch(branch.name)
	}

	override fun untrackBranches(branches: Set<String>) {
		branchQueries.transaction {
			branches.forEach {
				untrackBranch(it)
			}
		}
	}

	private fun untrackBranch(branchName: String) {
		branchQueries.transaction {
			branchQueries.bypass(branchName)
			branchQueries.remove(branchName)
		}
	}

	override fun renameBranch(branch: Branch, newName: String) {
		branchQueries.rename(
			oldName = branch.name,
			newName = newName,
		)
	}

	override fun updateParent(branch: String, parent: String) {
		branchQueries.updateParent(
			branch = branch,
			parent = parent,
		)
	}

	override fun updateParentSha(branch: Branch, parentSha: String) {
		branchQueries.updateParentSha(
			branch = branch.name,
			parentSha = parentSha,
		)
	}

	override fun updatePrNumber(
		branch: Branch,
		prNumber: Long,
	) {
		branchQueries.updatePrNumber(
			branch = branch.name,
			prNumber = prNumber,
		)
	}

	override fun setHasAskedToDelete(branch: Branch) {
		branchQueries.setHasAskedToDelete(branch.name)
	}

	private fun getTree(): TreeNode<Branch>? {
		val elements = branchQueries.selectAll().executeAsList()
		return if (elements.isEmpty()) {
			null
		} else {
			treeOf(
				elements = elements,
				keySelector = { it.name },
				parentSelector = { it.parent },
			)
		}
	}
}
