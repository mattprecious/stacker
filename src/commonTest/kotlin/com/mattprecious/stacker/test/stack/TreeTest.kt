package com.mattprecious.stacker.test.stack

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import com.mattprecious.stacker.stack.TreeNode
import com.mattprecious.stacker.stack.all
import com.mattprecious.stacker.stack.ancestors
import com.mattprecious.stacker.stack.descendants
import com.mattprecious.stacker.stack.treeOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TreeTest {
	@Test
	fun empty() = runTest {
		assertFailure { emptyList<Item>().asTree() }.hasMessage("elements must not be empty.")
	}

	@Test
	fun single() = runTest {
		val item = Item(key = "a", parent = null)
		val tree = listOf(item).asTree()!!
		assertThat(tree.value).isEqualTo(item)
		assertThat(tree.parent).isNull()
		assertThat(tree.children).isEmpty()
		assertThat(tree.root).isSameInstanceAs(tree)
	}

	@Test
	fun oneParentTwoChildren() = runTest {
		//   a
		//  / \
		// b   c
		val a = listOf(
			Item(key = "a", parent = null),
			Item(key = "b", parent = "a"),
			Item(key = "c", parent = "a"),
		).asTree()!!

		a.assertKey("a")
		assertThat(a.parent).isNull()
		assertThat(a.root).isSameInstanceAs(a)

		assertThat(a.children).hasSize(2)

		val b = a.children[0].assertKey("b")
		val c = a.children[1].assertKey("c")

		assertThat(b.children).isEmpty()
		assertThat(b.parent).isEqualTo(a)
		assertThat(b.root).isEqualTo(a)

		assertThat(c.children).isEmpty()
		assertThat(c.parent).isSameInstanceAs(a)
		assertThat(c.root).isSameInstanceAs(a)
	}

	@Test
	fun multiLevel() = runTest {
		//   a
		//  / \
		// b   d
		// |  / \
		// c e   f
		val a = listOf(
			Item(key = "a", parent = null),
			Item(key = "b", parent = "a"),
			Item(key = "c", parent = "b"),
			Item(key = "d", parent = "a"),
			Item(key = "e", parent = "d"),
			Item(key = "f", parent = "d"),
		).asTree()!!

		a.assertKey("a")
		assertThat(a.children).hasSize(2)
		assertThat(a.parent).isNull()
		assertThat(a.root).isSameInstanceAs(a)

		val b = a.children[0].assertKey("b")
		assertThat(b.parent).isSameInstanceAs(a)
		assertThat(b.root).isSameInstanceAs(a)

		val d = a.children[1].assertKey("d")

		val c = b.children.single().assertKey("c")
		assertThat(c.children).isEmpty()
		assertThat(c.parent).isSameInstanceAs(b)
		assertThat(c.root).isSameInstanceAs(a)

		assertThat(d.children).hasSize(2)
		assertThat(d.parent).isSameInstanceAs(a)
		assertThat(d.root).isSameInstanceAs(a)

		val e = d.children[0].assertKey("e")
		val f = d.children[1].assertKey("f")

		assertThat(e.children).isEmpty()
		assertThat(e.parent).isSameInstanceAs(d)
		assertThat(e.root).isSameInstanceAs(a)

		assertThat(f.children).isEmpty()
		assertThat(f.parent).isSameInstanceAs(d)
		assertThat(f.root).isSameInstanceAs(a)
	}

	@Test
	fun multipleRoots() = runTest {
		assertFailure {
			listOf(
				Item(key = "a", parent = null),
				Item(key = "b", parent = "a"),
				Item(key = "c", parent = null),
			).asTree()
		}.hasMessage("Multiple elements have a null parent: [a, c].")
	}

	@Test
	fun descendants() = runTest {
		//   a
		//  / \
		// b   d
		// |  / \
		// c e   f
		val a = Item(key = "a", parent = null)
		val b = Item(key = "b", parent = "a")
		val c = Item(key = "c", parent = "b")
		val d = Item(key = "d", parent = "a")
		val e = Item(key = "e", parent = "d")
		val f = Item(key = "f", parent = "d")

		val tree = listOf(a, b, c, d, e, f).asTree()!!

		val list = listOf(b, c, d, e, f)
		val descendants = tree.descendants

		var count = 0
		descendants.forEachIndexed { index, node ->
			count++
			assertThat(node.value).isSameInstanceAs(list[index])
		}
		assertThat(count).isEqualTo(list.size)

		// Ensure the Sequence can be consumed twice.
		assertThat(descendants.count()).isEqualTo(list.size)
	}

	@Test
	fun all() = runTest {
		//   a
		//  / \
		// b   d
		// |  / \
		// c e   f
		val a = Item(key = "a", parent = null)
		val b = Item(key = "b", parent = "a")
		val c = Item(key = "c", parent = "b")
		val d = Item(key = "d", parent = "a")
		val e = Item(key = "e", parent = "d")
		val f = Item(key = "f", parent = "d")

		val tree = listOf(a, b, c, d, e, f).asTree()!!

		val list = listOf(a, b, c, d, e, f)
		val all = tree.all

		var count = 0
		all.forEachIndexed { index, node ->
			count++
			assertThat(node.value).isSameInstanceAs(list[index])
		}
		assertThat(count).isEqualTo(list.size)

		// Ensure the Sequence can be consumed twice.
		assertThat(all.count()).isEqualTo(list.size)
	}

	@Test
	fun ancestors() = runTest {
		//   a
		//  / \
		// b   d
		// |  / \
		// c e   f
		val a = Item(key = "a", parent = null)
		val b = Item(key = "b", parent = "a")
		val c = Item(key = "c", parent = "b")
		val d = Item(key = "d", parent = "a")
		val e = Item(key = "e", parent = "d")
		val f = Item(key = "f", parent = "d")

		val tree = listOf(a, b, c, d, e, f).asTree()!!

		val list = listOf(d, a)
		val ancestors = tree.all.single { it.value.key == "e" }.ancestors

		var count = 0
		ancestors.forEachIndexed { index, node ->
			count++
			assertThat(node.value).isSameInstanceAs(list[index])
		}
		assertThat(count).isEqualTo(list.size)

		// Ensure the Sequence can be consumed twice.
		assertThat(ancestors.count()).isEqualTo(list.size)
	}

	private fun List<Item>.asTree() = treeOf(this, { it.key }, { it.parent })

	private data class Item(
		val key: String,
		val parent: String?,
	)

	private fun TreeNode<Item>.assertKey(key: String): TreeNode<Item> {
		assertThat(value.key).isEqualTo(key)
		return this
	}
}
