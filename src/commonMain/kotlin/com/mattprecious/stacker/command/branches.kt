package com.mattprecious.stacker.command

import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.stack.TreeNode

val TreeNode<Branch>.name: String
	get() = value.name

val TreeNode<Branch>.parentSha: String?
	get() = value.parentSha

internal fun TreeNode<Branch>.flattenUp(): List<TreeNode<Branch>> {
	return buildList {
		fun TreeNode<Branch>.addChildren() {
			children.forEach {
				add(it)
				it.addChildren()
			}
		}

		add(this@flattenUp)
		addChildren()
	}
}

internal fun TreeNode<Branch>.flattenDown(
	configManager: ConfigManager,
): List<TreeNode<Branch>> {
	val trunk = configManager.trunk
	val trailingTrunk = configManager.trailingTrunk
	return buildList {
		var branch = this@flattenDown
		while (branch.name != trailingTrunk && branch.name != trunk) {
			add(branch)
			branch = branch.parent ?: break
		}

		add(branch)
	}
}

internal class PrettyBranch(
	val branch: TreeNode<Branch>,
	val pretty: String,
)

internal fun TreeNode<Branch>.prettyTree(
	selected: TreeNode<Branch>? = null,
	filter: (TreeNode<Branch>) -> Boolean = { true },
): List<PrettyBranch> {
	return if (!filter(this)) {
		emptyList()
	} else {
		buildList {
			prettyTree(
				builder = this,
				inset = 0,
				treeWidth = treeWidth(filter),
				selected = selected,
				filter = filter,
			)
		}
	}
}

private fun TreeNode<Branch>.prettyTree(
	builder: MutableList<PrettyBranch>,
	inset: Int,
	treeWidth: Int,
	selected: TreeNode<Branch>? = null,
	filter: (TreeNode<Branch>) -> Boolean,
) {
	val filteredChildren = children.filter(filter)
	filteredChildren.forEachIndexed { index, child ->
		child.prettyTree(builder, inset + index, treeWidth, selected, filter)
	}

	val pretty = buildString {
		repeat(inset) { append("│ ") }

		if (this@prettyTree.name == selected?.name) {
			append("◉")
		} else {
			append("○")
		}

		val horizontalBranches = (filteredChildren.size - 1).coerceAtLeast(0)
		if (horizontalBranches > 0) {
			repeat(horizontalBranches - 1) { append("─┴") }
			append("─┘")
		}

		repeat(treeWidth - inset - horizontalBranches - 1) { append("  ") }

		append(" ")
		append(name)
	}

	builder += PrettyBranch(
		branch = this,
		pretty = pretty,
	)
}

private fun TreeNode<Branch>.treeWidth(
	filter: ((TreeNode<Branch>) -> Boolean),
): Int {
	return if (!filter(this)) {
		0
	} else {
		children.sumOf { it.treeWidth(filter) }.coerceAtLeast(1)
	}
}
