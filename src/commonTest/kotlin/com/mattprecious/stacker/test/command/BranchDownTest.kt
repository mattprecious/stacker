package com.mattprecious.stacker.test.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mattprecious.stacker.command.branch.branchCreate
import com.mattprecious.stacker.command.branch.branchDown
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

class BranchDownTest {
	@Test
	fun untracked() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", None) })
		gitCreateAndCheckoutBranch("change-a")

		testCommand({ branchDown() }) {
			awaitFrame(
				static = "Branch change-a is not tracked.",
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}
	}

	@Test
	fun singleBranch() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", None) })

		testCommand({ branchDown() }) {
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

		testCommand({ branchDown() }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("change-a")

		testCommand({ branchDown() }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("green-main")

		testCommand({ branchDown() }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("main")

		testCommand({ branchDown() }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("main")
	}

	@Test
	fun fork() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", None) })
		testCommand({ branchCreate("change-a") })
		gitCheckoutBranch("main")
		testCommand({ branchCreate("change-b") })

		testCommand({ branchDown() }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		assertThat(gitCurrentBranch()).isEqualTo("main")
	}
}
