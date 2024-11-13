package com.mattprecious.stacker.command

import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.stack.TreeNode

val TreeNode<Branch>.name: String
	get() = value.name

val TreeNode<Branch>.parentSha: String?
	get() = value.parentSha

internal class PrettyBranch(
	val branch: TreeNode<Branch>,
	val pretty: String,
)

internal fun TreeNode<Branch>.prettyTree(
	useFancySymbols: Boolean,
	selected: TreeNode<Branch>? = null,
	filter: (TreeNode<Branch>) -> Boolean = { true },
): List<PrettyBranch> {
	return if (!filter(this)) {
		emptyList()
	} else {
		buildList {
			prettyTree(
				builder = this,
				symbols = if (useFancySymbols) FancySymbols else NormalSymbols,
				hasParent = false,
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
	symbols: Symbols,
	hasParent: Boolean,
	inset: Int,
	treeWidth: Int,
	selected: TreeNode<Branch>? = null,
	filter: (TreeNode<Branch>) -> Boolean,
) {
	val filteredChildren = children.filter(filter)
	filteredChildren.forEachIndexed { index, child ->
		child.prettyTree(builder, symbols, true, inset + index, treeWidth, selected, filter)
	}

	val pretty = buildString {
		repeat(inset) { append(symbols.line) }

		append(
			symbols.branch(
				selected = name == selected?.name,
				childCount = children.size,
				hasParent = hasParent,
			),
		)

		val horizontalBranches = (filteredChildren.size - 1).coerceAtLeast(0)
		if (horizontalBranches > 0) {
			repeat(horizontalBranches - 1) { append(symbols.fork) }
			append(symbols.corner)
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

private sealed interface Symbols {
	val branchSoloSelected: String
	val branchSoloUnselected: String
	val branchBottomSelected: String
	val branchBottomUnselected: String
	val branchBottomForkSelected: String
	val branchBottomForkUnselected: String
	val branchMiddleSelected: String
	val branchMiddleUnselected: String
	val branchMiddleForkSelected: String
	val branchMiddleForkUnselected: String
	val branchTopSelected: String
	val branchTopUnselected: String
	val line: String
	val fork: String
	val corner: String

	fun branch(
		selected: Boolean,
		childCount: Int,
		hasParent: Boolean,
	): String {
		return when {
			hasParent -> when (childCount) {
				0 -> if (selected) branchTopSelected else branchTopUnselected
				1 -> if (selected) branchMiddleSelected else branchMiddleUnselected
				else -> if (selected) branchMiddleForkSelected else branchMiddleForkUnselected
			}
			else -> when (childCount) {
				0 -> if (selected) branchSoloSelected else branchSoloUnselected
				1 -> if (selected) branchBottomSelected else branchBottomUnselected
				else -> if (selected) branchBottomForkSelected else branchBottomForkUnselected
			}
		}
	}
}

private data object NormalSymbols : Symbols {
	override val branchSoloSelected = "●"
	override val branchSoloUnselected = "○"
	override val branchBottomSelected = "●"
	override val branchBottomUnselected = "○"
	override val branchBottomForkSelected = "●"
	override val branchBottomForkUnselected = "○"
	override val branchMiddleSelected = "●"
	override val branchMiddleUnselected = "○"
	override val branchMiddleForkSelected = "●"
	override val branchMiddleForkUnselected = "○"
	override val branchTopSelected = "●"
	override val branchTopUnselected = "○"
	override val line = "│ "
	override val fork = "─┴"
	override val corner = "─┘"
}

private data object FancySymbols : Symbols {
	override val branchSoloSelected = "\uF5EE"
	override val branchSoloUnselected = "\uF5EF"
	override val branchBottomSelected = "\uF5F8"
	override val branchBottomUnselected = "\uF5F9"
	override val branchBottomForkSelected = "\uF600"
	override val branchBottomForkUnselected = "\uF601"
	override val branchMiddleSelected = "\uF5FA"
	override val branchMiddleUnselected = "\uF5FB"
	override val branchMiddleForkSelected = "\uF604"
	override val branchMiddleForkUnselected = "\uF605"
	override val branchTopSelected = "\uF5F6"
	override val branchTopUnselected = "\uF5F7"
	override val line = "\uF5D1 "
	override val fork = "\uF5D0\uF5E3"
	override val corner = "\uF5D0\uF5D9"
}
