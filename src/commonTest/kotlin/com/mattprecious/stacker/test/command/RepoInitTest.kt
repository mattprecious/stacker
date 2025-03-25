package com.mattprecious.stacker.test.command

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.jakewharton.mosaic.terminal.KeyboardEvent
import com.mattprecious.stacker.command.branch.branchCreate
import com.mattprecious.stacker.command.repo.repoInit
import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.delegates.Optional
import com.mattprecious.stacker.delegates.Optional.Some
import com.mattprecious.stacker.test.util.Enter
import com.mattprecious.stacker.test.util.gitCommit
import com.mattprecious.stacker.test.util.gitCreateAndCheckoutBranch
import com.mattprecious.stacker.test.util.gitCreateBranch
import com.mattprecious.stacker.test.util.gitInit
import com.mattprecious.stacker.test.util.gitSetDefaultBranch
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
				|Select your trunk branch, which you open pull requests against:
				|❯ main
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: main",
				output = """
				|Select your trunk branch, which you open pull requests against:
				|❯ main
				""".trimMargin(),
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
				|Select your trunk branch, which you open pull requests against:
				|❯ main
				|  trunk
				""".trimMargin(),
			)
		}

		gitSetDefaultBranch("trunk")

		testCommand({ repoInit() }) {
			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:
				|  main
				|❯ trunk
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
				|Select your trunk branch, which you open pull requests against:
				|  a
				|❯ main
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: main",
				output = """
				|Select your trunk branch, which you open pull requests against:
				|  a
				|❯ main
				""".trimMargin(),
			)

			awaitFrame(
				output = "Do you use a trailing-trunk workflow? [y/N]:",
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Do you use a trailing-trunk workflow? [y/N]: ",
				output = "Do you use a trailing-trunk workflow? [y/N]:",
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
				|Select your trunk branch, which you open pull requests against:
				|  a
				|❯ main
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Up))

			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:
				|❯ a
				|  main
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))

			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:
				|  a
				|❯ main
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Up))

			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:
				|❯ a
				|  main
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: a",
				output = "Do you use a trailing-trunk workflow? [y/N]: ",
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Do you use a trailing-trunk workflow? [y/N]: ",
				output = "",
			)

			assertThat(awaitResult()).isTrue()
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
				|Select your trunk branch, which you open pull requests against:
				|  green-main
				|❯ main
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: main",
				output = "Do you use a trailing-trunk workflow? [y/N]: ",
			)

			sendText("y")

			awaitFrame("Do you use a trailing-trunk workflow? [y/N]: y")

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Do you use a trailing-trunk workflow? [y/N]: y",
				output = """
				|Select your trailing trunk branch, which you branch from:
				|❯ green-main
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

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
				|Select your trunk branch, which you open pull requests against:
				|  alpha
				|  green-trunk
				|  main
				|❯ trunk
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: trunk",
				output = "Do you use a trailing-trunk workflow? [Y/n]: ",
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Do you use a trailing-trunk workflow? [Y/n]: ",
				output = """
				|Select your trailing trunk branch, which you branch from:
				|  alpha
				|❯ green-trunk
				|  main
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
				|Select your trunk branch, which you open pull requests against:
				|  feature
				|❯ main
				|  trunk
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: trunk",
				output = "Do you use a trailing-trunk workflow? [y/N]: ",
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = """
					|Do you use a trailing-trunk workflow? [y/N]:
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
				|Select your trunk branch, which you open pull requests against:
				|  feature
				|  green-main
				|  green-trunk
				|❯ main
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: main",
				output = "Do you use a trailing-trunk workflow? [Y/n]: ",
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Do you use a trailing-trunk workflow? [Y/n]: ",
				output = """
				|Select your trailing trunk branch, which you branch from:
				|  feature
				|❯ green-main
				|  green-trunk
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

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
				|Select your trunk branch, which you open pull requests against:
				|❯ main
				|  trunk
				""".trimMargin(),
			)

			sendText("t")

			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against: t
				|❯ trunk
				""".trimMargin(),
			)
		}
	}
}
