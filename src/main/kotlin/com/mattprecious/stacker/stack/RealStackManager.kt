package com.mattprecious.stacker.stack

import com.mattprecious.stacker.db.RepoDatabase

class RealStackManager(
	db: RepoDatabase,
) : StackManager {
	private val branchQueries = db.branchQueries

	override fun getBase(): Branch? {
		val tree = getTree()
		val baseName = tree[null]?.single() ?: return null

		return StackBranch(
			name = baseName,
			tree = tree,
		)
	}

	override fun getBranch(branchName: String): Branch? {
		if (!branchQueries.contains(branchName).executeAsOne()) {
			return null
		}

		return StackBranch(
			name = branchName,
			tree = getTree(),
		)
	}

	override fun trackBranch(branchName: String, parentName: String?) {
		branchQueries.insert(
			name = branchName,
			parent = parentName,
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

	private fun getTree(): Map<String?, List<String>> {
		return branchQueries.selectAll().executeAsList().groupBy(
			keySelector = { it.parent },
			valueTransform = { it.name },
		)
	}

	private class StackBranch(
		override val name: String,
		tree: Map<String?, List<String>>,
	) : Branch {
		override val parent: Branch? by lazy {
			tree.firstNotNullOfOrNull { entry ->
				if (entry.value.contains(name)) {
					entry.key
				} else {
					null
				}?.let { StackBranch(it, tree) }
			}
		}

		override val children by lazy {
			tree[name]?.map { StackBranch(it, tree) } ?: emptyList()
		}

		override fun toString(): String {
			return name
		}
	}
}
