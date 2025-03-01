package com.mattprecious.stacker.test.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.jakewharton.mosaic.layout.KeyEvent
import com.mattprecious.stacker.command.branch.branchCreate
import com.mattprecious.stacker.command.branch.branchTop
import com.mattprecious.stacker.command.repo.repoInit
import com.mattprecious.stacker.delegates.Optional.None
import com.mattprecious.stacker.delegates.Optional.Some
import com.mattprecious.stacker.test.util.gitCheckoutBranch
import com.mattprecious.stacker.test.util.gitCommit
import com.mattprecious.stacker.test.util.gitCreateAndCheckoutBranch
import com.mattprecious.stacker.test.util.gitCurrentBranch
import com.mattprecious.stacker.test.util.gitInit
import com.mattprecious.stacker.test.util.s
import com.mattprecious.stacker.test.util.withTestEnvironment
import kotlin.test.Test

class BranchTopTest {
	@Test
	fun singleBranch() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", None) })

		testCommand({ branchTop() }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}
	}

	@Test
	fun linearStack() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		gitCreateAndCheckoutBranch("green-main")
		testCommand({ repoInit("main", Some("green-main")) })
		testCommand({ branchCreate("change-a") })
		testCommand({ branchCreate("change-b") })
		gitCheckoutBranch("main")

		testCommand({ branchTop() }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("change-b")

		testCommand({ branchTop() }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("change-b")
	}

	@Test
	fun fork() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", None) })
		testCommand({ branchCreate("change-a") })
		gitCheckoutBranch("main")
		testCommand({ branchCreate("change-b") })
		gitCheckoutBranch("main")

		testCommand({ branchTop() }) {
			awaitFrame(
				"""
				|Move up to:$s
				|❯ change-a $s
				|  change-b $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Move up to: change-a",
				output = "",
			)

			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("change-a")

		gitCheckoutBranch("main")

		testCommand({ branchTop() }) {
			awaitFrame(
				"""
				|Move up to:$s
				|❯ change-a $s
				|  change-b $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))

			awaitFrame(
				"""
				|Move up to:$s
				|  change-a $s
				|❯ change-b $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Move up to: change-b",
				output = "",
			)

			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("change-b")
	}

	@Test
	fun multiFork() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", None) })
		testCommand({ branchCreate("change-a") })
		gitCheckoutBranch("main")
		testCommand({ branchCreate("change-b") })
		testCommand({ branchCreate("change-c") })
		testCommand({ branchCreate("change-d") })
		gitCheckoutBranch("change-c")
		testCommand({ branchCreate("change-e") })
		gitCheckoutBranch("change-c")
		testCommand({ branchCreate("change-f") })
		gitCheckoutBranch("main")
		testCommand({ branchCreate("change-g") })
		gitCheckoutBranch("main")

		testCommand({ branchTop() }) {
			awaitFrame(
				"""
				|Move up to:$s
				|❯ change-a $s
				|  change-d $s
				|  change-e $s
				|  change-f $s
				|  change-g $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Move up to: change-a",
				output = "",
			)

			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("change-a")
	}
}
