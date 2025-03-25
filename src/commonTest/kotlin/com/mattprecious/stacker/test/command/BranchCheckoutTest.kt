package com.mattprecious.stacker.test.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.jakewharton.mosaic.terminal.KeyboardEvent
import com.mattprecious.stacker.command.branch.branchCheckout
import com.mattprecious.stacker.command.branch.branchCreate
import com.mattprecious.stacker.command.repo.repoInit
import com.mattprecious.stacker.delegates.Optional
import com.mattprecious.stacker.test.util.Enter
import com.mattprecious.stacker.test.util.gitCheckoutBranch
import com.mattprecious.stacker.test.util.gitCommit
import com.mattprecious.stacker.test.util.gitCreateAndCheckoutBranch
import com.mattprecious.stacker.test.util.gitCurrentBranch
import com.mattprecious.stacker.test.util.gitInit
import com.mattprecious.stacker.test.util.withTestEnvironment
import kotlin.test.Test

class BranchCheckoutTest {
	@Test
	fun single() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })

		testCommand({ branchCheckout(null) }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("main")
	}

	@Test
	fun singleFromUntracked() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		gitCreateAndCheckoutBranch("change-a")

		testCommand({ branchCheckout(null) }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("main")
	}

	@Test
	fun simple() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("change-a") })
		testCommand({ branchCreate("change-b") })
		gitCheckoutBranch("main")
		testCommand({ branchCreate("change-c") })
		gitCheckoutBranch("change-a")

		testCommand({ branchCheckout(null) }) {
			awaitFrame(
				"""
				|Checkout a branch:
				|  ○   change-b
				|❯ ○   change-a
				|  │ ○ change-c
				|  ○─┘ main
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Up))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Checkout a branch: change-b",
				output = "",
			)

			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("change-b")
	}

	@Test
	fun simpleWithArgument() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("change-a") })
		testCommand({ branchCreate("change-b") })
		gitCheckoutBranch("main")
		testCommand({ branchCreate("change-c") })
		gitCheckoutBranch("change-a")

		testCommand({ branchCheckout("change-c") }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("change-c")
	}

	@Test
	fun unknownBranch() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("change-a") })

		testCommand({ branchCheckout("change-c") }) {
			awaitFrame(
				static = "change-c does not match any branches known to git.",
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}

		assertThat(gitCurrentBranch()).isEqualTo("change-a")
	}

	@Test
	fun filteringIsEnabled() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("change-a") })
		testCommand({ branchCreate("change-b") })
		gitCheckoutBranch("main")
		testCommand({ branchCreate("change-c") })
		gitCheckoutBranch("change-a")

		testCommand({ branchCheckout(null) }) {
			awaitFrame(
				"""
				|Checkout a branch:
				|  ○   change-b
				|❯ ○   change-a
				|  │ ○ change-c
				|  ○─┘ main
				""".trimMargin(),
			)

			sendText("-c")

			awaitFrame(
				"""
				|Checkout a branch: -c
				|❯ │ ○ change-c
				""".trimMargin(),
			)
		}
	}
}
