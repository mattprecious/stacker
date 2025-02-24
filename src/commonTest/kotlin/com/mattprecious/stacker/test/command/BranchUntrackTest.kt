package com.mattprecious.stacker.test.command

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mattprecious.stacker.command.branch.branchCreate
import com.mattprecious.stacker.command.branch.branchUntrack
import com.mattprecious.stacker.command.repo.repoInit
import com.mattprecious.stacker.delegates.Optional
import com.mattprecious.stacker.test.util.gitCommit
import com.mattprecious.stacker.test.util.gitCreateAndCheckoutBranch
import com.mattprecious.stacker.test.util.gitCreateBranch
import com.mattprecious.stacker.test.util.gitCurrentBranch
import com.mattprecious.stacker.test.util.gitInit
import com.mattprecious.stacker.test.util.withTestEnvironment
import kotlin.test.Test

class BranchUntrackTest {
	@Test
	fun notTracked() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		gitCreateAndCheckoutBranch("change-a")

		testCommand({ branchUntrack(null) }) {
			awaitFrame(
				static = "Branch change-a is already not tracked.",
				output = "",
			)

			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
				.containsExactly("main")
		}
	}

	@Test
	fun withChildren() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("change-a") })
		testCommand({ branchCreate("change-b") })

		testCommand({ branchUntrack("change-a") }) {
			awaitFrame(
				static = "Branch change-a has children. Please retarget or untrack them.",
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
				.containsExactlyInAnyOrder("main", "change-a", "change-b")
		}
	}

	@Test
	fun singleBranch() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("change-a") })

		testCommand({ branchUntrack(null) }) {
			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
				.containsExactly("main")
		}

		// Does not change branch.
		assertThat(gitCurrentBranch()).isEqualTo("change-a")
	}

	@Test
	fun cannotUntrackTrunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })

		testCommand({ branchUntrack(null) }) {
			awaitFrame(
				static = "Cannot untrack trunk branch.",
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
				.containsExactly("main")
		}
	}

	@Test
	fun cannotUntrackTrailingTrunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		gitCreateBranch("green-main")
		testCommand({ repoInit("main", Optional.Some("green-main")) })

		testCommand({ branchUntrack("green-main") }) {
			awaitFrame(
				static = "Cannot untrack trailing trunk branch.",
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
				.containsExactly("main", "green-main")
		}
	}
}
