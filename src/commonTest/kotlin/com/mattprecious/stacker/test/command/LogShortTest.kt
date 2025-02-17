package com.mattprecious.stacker.test.command

import app.cash.burst.Burst
import com.mattprecious.stacker.command.log.logShort
import com.mattprecious.stacker.test.util.gitCheckoutBranch
import com.mattprecious.stacker.test.util.gitCommit
import com.mattprecious.stacker.test.util.gitCreateAndCheckoutBranch
import com.mattprecious.stacker.test.util.gitCreateBranch
import com.mattprecious.stacker.test.util.gitInit
import com.mattprecious.stacker.test.util.s
import com.mattprecious.stacker.test.util.withTestEnvironment
import kotlin.test.Test

@Burst
class LogShortTest {
	@Test
	fun errorsIfNotInitialized() = withTestEnvironment {
		gitInit()

		testCommand({ logShort() }) {
			awaitFrame(
				static = "Stacker must be initialized first. Please run st repo init.",
				output = "",
			)
		}
	}

	private enum class NoRestackCase(
		val branches: List<Pair<String, String?>>,
		val headBranch: String = "main",
		val output: String,
	) {
		One(
			branches = listOf("main" to null),
			output = "● main",
		),
		Two(
			branches = listOf(
				"main" to null,
				"a" to "main",
			),
			output = """
				|○ a  $s
				|● main
			""".trimMargin(),
		),
		TwoWithSecondCheckedOut(
			branches = listOf(
				"main" to null,
				"a" to "main",
			),
			headBranch = "a",
			output = """
				|● a  $s
				|○ main
			""".trimMargin(),
		),
		Three(
			branches = listOf(
				"main" to null,
				"a" to "main",
				"b" to "a",
			),
			output = """
				|○ b  $s
				|○ a  $s
				|● main
			""".trimMargin(),
		),
		TwoChildren(
			branches = listOf(
				"main" to null,
				"a" to "main",
				"b" to "main",
			),
			output = """
				|○   a  $s
				|│ ○ b  $s
				|●─┘ main
			""".trimMargin(),
		),
		ThreeChildren(
			branches = listOf(
				"main" to null,
				"a" to "main",
				"b" to "main",
				"c" to "main",
			),
			output = """
				|○     a  $s
				|│ ○   b  $s
				|│ │ ○ c  $s
				|●─┴─┘ main
			""".trimMargin(),
		),
		Complex(
			branches = listOf(
				"main" to null,
				"a" to "main",
				"b" to "main",
				"c" to "main",
				"d" to "a",
				"e" to "b",
				"f" to "c",
				"g" to "f",
				"h" to "f",
				"i" to "d",
				"j" to "e",
				"k" to "j",
			),
			headBranch = "f",
			output = """
				|○       i  $s
				|○       d  $s
				|○       a  $s
				|│ ○     k  $s
				|│ ○     j  $s
				|│ ○     e  $s
				|│ ○     b  $s
				|│ │ ○   g  $s
				|│ │ │ ○ h  $s
				|│ │ ●─┘ f  $s
				|│ │ ○   c  $s
				|○─┴─┘   main
			""".trimMargin(),
		),
	}

	@Test
	private fun noRestacksRequired(
		case: NoRestackCase,
	) = withTestEnvironment {
		gitInit()
		val sha = gitCommit("Empty")
		case.branches.forEach {
			if (it.first != "main") {
				gitCreateBranch(it.first)
			}
		}

		gitCheckoutBranch(case.headBranch)

		withDatabase(requireExists = false) { db ->
			db.repoConfigQueries.insert(
				trunk = "main",
				trailingTrunk = null,
			)

			case.branches.forEach {
				db.branchQueries.insert(
					name = it.first,
					parent = it.second,
					parentSha = sha,
					prNumber = null,
				)
			}
		}

		testCommand({ logShort() }) {
			awaitFrame(
				static = case.output,
				output = "",
			)
		}
	}

	@Test
	fun oneChildNeedsRestack() = withTestEnvironment {
		gitInit()
		val sha = gitCommit("Empty")
		gitCommit("Second")
		gitCreateBranch("a", sha)

		withDatabase(requireExists = false) { db ->
			db.repoConfigQueries.insert(
				trunk = "main",
				trailingTrunk = null,
			)

			db.branchQueries.insert(
				name = "main",
				parent = null,
				parentSha = null,
				prNumber = null,
			)

			db.branchQueries.insert(
				name = "a",
				parent = "main",
				parentSha = sha,
				prNumber = null,
			)
		}

		testCommand({ logShort() }) {
			awaitFrame(
				static = """
					|○ a (needs restack)
					|● main            $s
				""".trimMargin(),
				output = "",
			)
		}
	}

	@Test
	fun restackRequiredDoesNotPropagateUpwards() = withTestEnvironment {
		gitInit()
		val sha1 = gitCommit("Empty")
		gitCreateAndCheckoutBranch("a")
		val sha2 = gitCommit("Second")
		gitCreateAndCheckoutBranch("b")
		gitCommit("Third")
		gitCheckoutBranch("main")
		gitCommit("Fourth")

		withDatabase(requireExists = false) { db ->
			db.repoConfigQueries.insert(
				trunk = "main",
				trailingTrunk = null,
			)

			db.branchQueries.insert(
				name = "main",
				parent = null,
				parentSha = null,
				prNumber = null,
			)

			db.branchQueries.insert(
				name = "a",
				parent = "main",
				parentSha = sha1,
				prNumber = null,
			)

			db.branchQueries.insert(
				name = "b",
				parent = "a",
				parentSha = sha2,
				prNumber = null,
			)
		}

		testCommand({ logShort() }) {
			awaitFrame(
				static = """
					|○ b               $s
					|○ a (needs restack)
					|● main            $s
				""".trimMargin(),
				output = "",
			)
		}
	}

	@Test
	fun trunkBranchesNeverNeedRestack() = withTestEnvironment {
		gitInit()
		val sha = gitCommit("Empty")
		gitCommit("Second")
		gitCreateBranch("green-main", sha)

		withDatabase(requireExists = false) { db ->
			db.repoConfigQueries.insert(
				trunk = "main",
				trailingTrunk = "green-main",
			)

			db.branchQueries.insert(
				name = "main",
				parent = null,
				parentSha = null,
				prNumber = null,
			)

			db.branchQueries.insert(
				name = "green-main",
				parent = "main",
				parentSha = sha,
				prNumber = null,
			)
		}

		testCommand({ logShort() }) {
			awaitFrame(
				static = """
					|○ green-main
					|● main     $s
				""".trimMargin(),
				output = "",
			)
		}
	}
}
