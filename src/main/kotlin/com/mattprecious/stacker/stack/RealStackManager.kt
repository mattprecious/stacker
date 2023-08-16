package com.mattprecious.stacker.stack

import com.mattprecious.stacker.db.RepoDatabase

class RealStackManager(
	db: RepoDatabase,
) : StackManager {
	private val branchQueries = db.branchQueries

	override val trackedBranchNames: List<String>
		get() = branchQueries.names().executeAsList()

	override fun getBase(): Branch? {
		val tree = getTree()
		val base = tree.root ?: return null

		return StackBranch(
			name = base.name,
			parentSha = base.parentSha,
			tree = tree,
		)
	}

	override fun getBranch(branchName: String): Branch? {
		val branch = branchQueries.select(branchName).executeAsOneOrNull() ?: return null

		return StackBranch(
			name = branch.name,
			parentSha = branch.parentSha,
			tree = getTree(),
		)
	}

	override fun trackBranch(branchName: String, parentName: String?, parentSha: String?) {
		branchQueries.insert(
			name = branchName,
			parent = parentName,
			parentSha = parentSha,
		)
	}

	override fun untrackBranch(branch: Branch) {
		require(branch.children.isEmpty()) {
			"Branch has children: ${branch.children}."
		}

		branchQueries.remove(branch.name)
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

	private fun getTree(): Tree {
		val branches = branchQueries.selectAll().executeAsList()
		val treeBranches = branches.map { TreeBranch(it.name, it.parentSha) }.associateBy { it.name }
		return Tree(
			root = treeBranches[branches.single { it.parent == null }.name],
			parents = branches.associateBy({ it.name }, { treeBranches[it.parent] }),
			children = branches.groupBy({ it.parent }, { treeBranches[it.name]!! }),
		)
	}

	private class Tree(
		val root: TreeBranch?,
		val parents: Map<String, TreeBranch?>,
		val children: Map<String?, List<TreeBranch>>,
	)

	private data class TreeBranch(
		val name: String,
		val parentSha: String?,
	)

	private class StackBranch(
		override val name: String,
		override val parentSha: String?,
		tree: Tree,
	) : Branch {
		override val parent: Branch? by lazy {
			tree.parents[name]?.let { StackBranch(it.name, it.parentSha, tree) }
		}

		override val children by lazy {
			tree.children[name]?.map { StackBranch(it.name, it.parentSha, tree) } ?: emptyList()
		}

		override fun toString(): String {
			return name
		}
	}
}
