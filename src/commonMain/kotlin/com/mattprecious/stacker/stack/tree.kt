package com.mattprecious.stacker.stack

import androidx.compose.runtime.Stable

fun <K : Any, T : Any> treeOf(
	elements: Collection<T>,
	keySelector: (T) -> K,
	parentSelector: (T) -> K?,
): TreeNode<T> {
	require(elements.isNotEmpty()) {
		"elements must not be empty."
	}

	val elementsByKey = elements.associateBy(keySelector)
	val parents = elementsByKey.mapValues {
		elements.indexOf(elementsByKey[parentSelector(it.value)])
	}
	val children = elements.groupBy(parentSelector) { elements.indexOf(it) }

	require(children[null]!!.size == 1) {
		val elementsList = elements.toList()
		val roots = children[null]!!.map { keySelector(elementsList[it]) }
		"Multiple elements have a null parent: $roots."
	}

	return Tree(
		elements = elements,
		keySelector,
		parentMap = parents,
		childMap = children,
	).root
}

internal class Tree<K : Any, T : Any>(
	elements: Collection<T>,
	private val keySelector: (T) -> K,
	private val parentMap: Map<K, Int>,
	private val childMap: Map<K?, List<Int>>,
) {
	private val nodes = elements.map { TreeNode(this, it) }

	val root: TreeNode<T> = nodes[childMap[null]!!.single()]

	fun getParent(element: T): TreeNode<T>? {
		val index = parentMap[keySelector(element)]!!
		return if (index == -1) {
			null
		} else {
			nodes[index]
		}
	}

	fun getChildren(element: T): List<TreeNode<T>> {
		return childMap[keySelector(element)]?.map { nodes[it] } ?: emptyList()
	}
}

@Stable
class TreeNode<T : Any> internal constructor(
	private val tree: Tree<*, T>,
	val value: T,
) {
	val root: TreeNode<T>
		get() = tree.root

	val parent: TreeNode<T>?
		get() = tree.getParent(value)

	val children: List<TreeNode<T>>
		get() = tree.getChildren(value)
}

/** Returns a sequence over the parent chain of this node. */
val <T : Any> TreeNode<T>.ancestors
	get() = generateSequence(parent) { it.parent }

/** Returns a sequence over the children of this node. This performs a depth-first traversal. */
val <T : Any> TreeNode<T>.descendants: Sequence<TreeNode<T>>
	get() = sequence { children.forEach { yieldAll(it.all) } }

/** Returns a sequence over this node and its descendants. */
val <T : Any> TreeNode<T>.all: Sequence<TreeNode<T>>
	get() = sequence {
		yield(this@all)
		yieldAll(descendants)
	}
