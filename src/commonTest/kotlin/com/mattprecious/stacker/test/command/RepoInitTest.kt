package com.mattprecious.stacker.test.command

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.jakewharton.mosaic.layout.KeyEvent
import com.mattprecious.stacker.command.branch.branchCreate
import com.mattprecious.stacker.command.repo.repoInit
import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.delegates.Optional
import com.mattprecious.stacker.delegates.Optional.Some
import com.mattprecious.stacker.test.util.gitCommit
import com.mattprecious.stacker.test.util.gitCreateAndCheckoutBranch
import com.mattprecious.stacker.test.util.gitCreateBranch
import com.mattprecious.stacker.test.util.gitInit
import com.mattprecious.stacker.test.util.gitSetDefaultBranch
import com.mattprecious.stacker.test.util.s
import com.mattprecious.stacker.test.util.withTestEnvironment
import kotlin.test.Test

class RepoInitTest {
	@Test
	fun errorsInitializingEmptyRepo() = withTestEnvironment {
		gitInit()

		testCommand({ repoInit() }) {
			awaitFrame(
				static = "Stacker cannot be initialized in a completely empty repository. " +
					"Please make a commit first.",
				output = "",
			)
			assertThat(awaitResult()).isFalse()
		}

		withDatabase {
			assertThat(it.repoConfigQueries.initialized().executeAsOne()).isFalse()
			assertThat(it.repoConfigQueries.trunk().executeAsOneOrNull()).isNull()
			assertThat(it.repoConfigQueries.trailingTrunk().executeAsOneOrNull()).isNull()

			assertThat(it.branchQueries.selectAll().executeAsList()).isEmpty()
		}
	}

	@Test
	fun successfullyInitsWithOneBranch() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")

		assertThat(fileSystem.exists(defaultDbPath)).isFalse()

		testCommand({ repoInit() }) {
			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|❯ main                                                         $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: main",
				output = "",
			)

			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.repoConfigQueries.initialized().executeAsOne()).isTrue()
			assertThat(it.repoConfigQueries.trunk().executeAsOne()).isEqualTo("main")
			assertThat(it.repoConfigQueries.trailingTrunk().executeAsOne().trailingTrunk).isNull()

			assertThat(it.branchQueries.selectAll().executeAsList()).containsExactly(
				Branch(
					name = "main",
					parent = null,
					parentSha = null,
					prNumber = null,
					hasAskedToDelete = false,
				),
			)
		}
	}

	@Test
	fun defaultBranchIsPreselected() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		gitCreateAndCheckoutBranch("trunk")

		testCommand({ repoInit() }) {
			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|❯ main                                                         $s
				|  trunk                                                        $s
				""".trimMargin(),
			)
		}

		gitSetDefaultBranch("trunk")

		testCommand({ repoInit() }) {
			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|  main                                                         $s
				|❯ trunk                                                        $s
				""".trimMargin(),
			)
		}
	}

	@Test
	fun successfullyInitsWithNoTrailingTrunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		gitCreateAndCheckoutBranch("a")

		assertThat(fileSystem.exists(defaultDbPath)).isFalse()

		testCommand({ repoInit() }) {
			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|  a                                                            $s
				|❯ main                                                         $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: main",
				output = "Do you use a trailing-trunk workflow? [y/N]: ",
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Do you use a trailing-trunk workflow? [y/N]: ",
				output = "",
			)

			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.repoConfigQueries.initialized().executeAsOne()).isTrue()
			assertThat(it.repoConfigQueries.trunk().executeAsOne()).isEqualTo("main")
			assertThat(it.repoConfigQueries.trailingTrunk().executeAsOne().trailingTrunk).isNull()

			assertThat(it.branchQueries.selectAll().executeAsList()).containsExactly(
				Branch(
					name = "main",
					parent = null,
					parentSha = null,
					prNumber = null,
					hasAskedToDelete = false,
				),
			)
		}
	}

	@Test
	fun canSelectOtherTrunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		gitCreateAndCheckoutBranch("a")

		testCommand({ repoInit() }) {
			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|  a                                                            $s
				|❯ main                                                         $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowUp"))

			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|❯ a                                                            $s
				|  main                                                         $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))

			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|  a                                                            $s
				|❯ main                                                         $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowUp"))

			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|❯ a                                                            $s
				|  main                                                         $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: a",
				output = "Do you use a trailing-trunk workflow? [y/N]: ",
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Do you use a trailing-trunk workflow? [y/N]: ",
				output = "",
			)
		}

		withDatabase {
			assertThat(it.repoConfigQueries.initialized().executeAsOne()).isTrue()
			assertThat(it.repoConfigQueries.trunk().executeAsOne()).isEqualTo("a")
			assertThat(it.repoConfigQueries.trailingTrunk().executeAsOne().trailingTrunk).isNull()

			assertThat(it.branchQueries.selectAll().executeAsList()).containsExactly(
				Branch(
					name = "a",
					parent = null,
					parentSha = null,
					prNumber = null,
					hasAskedToDelete = false,
				),
			)
		}
	}

	@Test
	fun successfullyInitsWithTrailingTrunk() = withTestEnvironment {
		gitInit()
		val sha = gitCommit("Empty")
		gitCreateAndCheckoutBranch("green-main")

		assertThat(fileSystem.exists(defaultDbPath)).isFalse()

		testCommand({ repoInit() }) {
			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|  green-main                                                   $s
				|❯ main                                                         $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: main",
				output = "Do you use a trailing-trunk workflow? [y/N]: ",
			)

			sendText("y")

			awaitFrame("Do you use a trailing-trunk workflow? [y/N]: y")

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Do you use a trailing-trunk workflow? [y/N]: y",
				output = """
				|Select your trailing trunk branch, which you branch from:$s
				|❯ green-main                                             $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Select your trailing trunk branch, which you branch from: green-main",
				output = "",
			)

			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.repoConfigQueries.initialized().executeAsOne()).isTrue()
			assertThat(it.repoConfigQueries.trunk().executeAsOne()).isEqualTo("main")
			assertThat(it.repoConfigQueries.trailingTrunk().executeAsOne().trailingTrunk)
				.isEqualTo("green-main")

			assertThat(it.branchQueries.selectAll().executeAsList()).containsExactlyInAnyOrder(
				Branch(
					name = "main",
					parent = null,
					parentSha = null,
					prNumber = null,
					hasAskedToDelete = false,
				),
				Branch(
					name = "green-main",
					parent = "main",
					parentSha = sha.long,
					prNumber = null,
					hasAskedToDelete = false,
				),
			)
		}
	}

	@Test
	fun existingSettingsArePreselected() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		gitCreateAndCheckoutBranch("alpha")
		gitCreateAndCheckoutBranch("trunk")
		gitCreateAndCheckoutBranch("green-trunk")
		testCommand({ repoInit("trunk", Some("green-trunk")) })

		testCommand({ repoInit() }) {
			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|  alpha                                                        $s
				|  green-trunk                                                  $s
				|  main                                                         $s
				|❯ trunk                                                        $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: trunk",
				output = "Do you use a trailing-trunk workflow? [Y/n]: ",
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Do you use a trailing-trunk workflow? [Y/n]: ",
				output = """
				|Select your trailing trunk branch, which you branch from:$s
				|  alpha                                                  $s
				|❯ green-trunk                                            $s
				|  main                                                   $s
				""".trimMargin(),
			)
		}
	}

	@Test
	fun changingTrunkFailsIfChildren() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("feature") })
		gitCreateBranch("trunk")

		testCommand({ repoInit() }) {
			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|  feature                                                      $s
				|❯ main                                                         $s
				|  trunk                                                        $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: trunk",
				output = "Do you use a trailing-trunk workflow? [y/N]: ",
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = """
					|Do you use a trailing-trunk workflow? [y/N]:$s
					|Cannot change trunk. Current trunk branch main has children.
				""".trimMargin(),
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}
	}

	@Test
	fun changingTrailingTrunkFailsIfChildren() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		gitCreateAndCheckoutBranch("green-main")
		gitCreateBranch("green-trunk")
		testCommand({ repoInit("main", Some("green-main")) })
		testCommand({ branchCreate("feature") })

		testCommand({ repoInit() }) {
			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|  feature                                                      $s
				|  green-main                                                   $s
				|  green-trunk                                                  $s
				|❯ main                                                         $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: main",
				output = "Do you use a trailing-trunk workflow? [Y/n]: ",
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Do you use a trailing-trunk workflow? [Y/n]: ",
				output = """
				|Select your trailing trunk branch, which you branch from:$s
				|  feature                                                $s
				|❯ green-main                                             $s
				|  green-trunk                                            $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = """
					|Select your trailing trunk branch, which you branch from: green-trunk
					|Cannot change trailing trunk. Current trailing trunk branch green-main has children.
				""".trimMargin(),
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}
	}

	@Test
	fun filteringIsEnabled() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		gitCreateAndCheckoutBranch("trunk")

		testCommand({ repoInit() }) {
			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|❯ main                                                         $s
				|  trunk                                                        $s
				""".trimMargin(),
			)

			sendText("t")

			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against: t
				|❯ trunk                                                         $s
				""".trimMargin(),
			)
		}
	}
}
