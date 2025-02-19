package com.mattprecious.stacker.test.command

import app.cash.burst.Burst
import com.mattprecious.stacker.command.branch.branchCreate
import com.mattprecious.stacker.command.log.logShort
import com.mattprecious.stacker.command.repo.repoInit
import com.mattprecious.stacker.delegates.Optional
import com.mattprecious.stacker.delegates.Optional.Some
import com.mattprecious.stacker.test.util.gitCheckoutBranch
import com.mattprecious.stacker.test.util.gitCommit
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
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })

		case.branches.forEach {
			val parent = it.second
			if (parent != null) {
				gitCheckoutBranch(parent)
				testCommand({ branchCreate(it.first) })
			}
		}

		gitCheckoutBranch(case.headBranch)

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
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("a") })
		gitCheckoutBranch("main")
		gitCommit("Second")

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
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("a") })
		gitCommit("Second")
		testCommand({ branchCreate("b") })
		gitCommit("Third")
		gitCheckoutBranch("main")
		gitCommit("Fourth")

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
		gitCommit("Empty")
		gitCreateBranch("green-main")
		testCommand({ repoInit("main", Some("green-main")) })
		gitCommit("Second")

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
