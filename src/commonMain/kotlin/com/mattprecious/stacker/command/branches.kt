package com.mattprecious.stacker.command

import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.stack.Branch

internal fun Branch.flattenUp(): List<Branch> {
	return buildList {
		fun Branch.addChildren() {
			children.forEach {
				add(it)
				it.addChildren()
			}
		}

		add(this@flattenUp)
		addChildren()
	}
}

internal fun Branch.flattenDown(
	configManager: ConfigManager,
): List<Branch> {
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
	val branch: Branch,
	val pretty: String,
)

internal fun Branch.prettyTree(
	selected: Branch? = null,
	filter: (Branch) -> Boolean = { true },
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

private fun Branch.prettyTree(
	builder: MutableList<PrettyBranch>,
	inset: Int,
	treeWidth: Int,
	selected: Branch? = null,
	filter: (Branch) -> Boolean,
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

private fun Branch.treeWidth(
	filter: ((Branch) -> Boolean),
): Int {
	return if (!filter(this)) {
		0
	} else {
		children.sumOf { it.treeWidth(filter) }.coerceAtLeast(1)
	}
}
