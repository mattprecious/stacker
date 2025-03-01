package com.mattprecious.stacker.test.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mattprecious.stacker.command.branch.branchBottom
import com.mattprecious.stacker.command.branch.branchCreate
import com.mattprecious.stacker.command.repo.repoInit
import com.mattprecious.stacker.delegates.Optional.None
import com.mattprecious.stacker.delegates.Optional.Some
import com.mattprecious.stacker.test.util.gitCheckoutBranch
import com.mattprecious.stacker.test.util.gitCommit
import com.mattprecious.stacker.test.util.gitCreateAndCheckoutBranch
import com.mattprecious.stacker.test.util.gitCurrentBranch
import com.mattprecious.stacker.test.util.gitInit
import com.mattprecious.stacker.test.util.withTestEnvironment
import kotlin.test.Test

class BranchBottomTest {
	@Test
	fun trunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", None) })

		testCommand({ branchBottom() }) {
			awaitFrame(
				static = "Not in a stack.",
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}
	}

	@Test
	fun trailingTrunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		gitCreateAndCheckoutBranch("green-main")
		testCommand({ repoInit("main", Some("green-main")) })

		testCommand({ branchBottom() }) {
			awaitFrame(
				static = "Not in a stack.",
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}

		assertThat(gitCurrentBranch()).isEqualTo("green-main")
	}

	@Test
	fun singleOnTrunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", None) })
		testCommand({ branchCreate("change-a") })

		testCommand({ branchBottom() }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("change-a")
	}

	@Test
	fun singleOnTrailingTrunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		gitCreateAndCheckoutBranch("green-main")
		testCommand({ repoInit("main", Some("green-main")) })
		testCommand({ branchCreate("change-a") })

		testCommand({ branchBottom() }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("change-a")
	}

	@Test
	fun linearStack() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		gitCreateAndCheckoutBranch("green-main")
		testCommand({ repoInit("main", Some("green-main")) })
		testCommand({ branchCreate("change-a") })
		testCommand({ branchCreate("change-b") })
		testCommand({ branchCreate("change-c") })

		testCommand({ branchBottom() }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("change-a")
	}

	@Test
	fun fork() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", None) })
		testCommand({ branchCreate("change-a") })
		gitCheckoutBranch("main")
		testCommand({ branchCreate("change-b") })
		testCommand({ branchCreate("change-c") })

		testCommand({ branchBottom() }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("change-b")
	}
}
