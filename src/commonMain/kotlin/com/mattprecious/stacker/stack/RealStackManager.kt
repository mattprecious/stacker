package com.mattprecious.stacker.stack

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

	override fun updateParent(branch: Branch, parent: Branch) {
		branchQueries.updateParent(
			branch = branch.name,
			parent = parent.name,
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

	private fun getTree(): TreeNode<Branch>? {
		return treeOf(
			elements = branchQueries.selectAll().executeAsList(),
			keySelector = { it.name },
			parentSelector = { it.parent },
		)
	}
}
