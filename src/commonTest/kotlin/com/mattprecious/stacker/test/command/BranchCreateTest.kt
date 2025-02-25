package com.mattprecious.stacker.test.command

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mattprecious.stacker.command.branch.branchCreate
import com.mattprecious.stacker.command.repo.repoInit
import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.delegates.Optional
import com.mattprecious.stacker.test.util.gitCommit
import com.mattprecious.stacker.test.util.gitCreateAndCheckoutBranch
import com.mattprecious.stacker.test.util.gitCreateBranch
import com.mattprecious.stacker.test.util.gitInit
import com.mattprecious.stacker.test.util.withTestEnvironment
import kotlin.test.Test

class BranchCreateTest {
	@Test
	fun branchFromTrunk() = withTestEnvironment {
		gitInit()
		val mainSha = gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })

		testCommand({ branchCreate("change-a") }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList()).containsExactlyInAnyOrder(
				Branch(
					name = "main",
					parent = null,
					parentSha = null,
					prNumber = null,
					hasAskedToDelete = false,
				),
				Branch(
					name = "change-a",
					parent = "main",
					parentSha = mainSha.long,
					prNumber = null,
					hasAskedToDelete = false,
				),
			)
		}
	}

	@Test
	fun branchFromNonTrunkBranch() = withTestEnvironment {
		gitInit()
		val mainSha = gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("change-a") })
		val parentSha = gitCommit("Change A")

		testCommand({ branchCreate("change-b") }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList()).containsExactlyInAnyOrder(
				Branch(
					name = "main",
					parent = null,
					parentSha = null,
					prNumber = null,
					hasAskedToDelete = false,
				),
				Branch(
					name = "change-a",
					parent = "main",
					parentSha = mainSha.long,
					prNumber = null,
					hasAskedToDelete = false,
				),
				Branch(
					name = "change-b",
					parent = "change-a",
					parentSha = parentSha.long,
					prNumber = null,
					hasAskedToDelete = false,
				),
			)
		}
	}

	@Test
	fun branchFromUntrackedBranch() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		gitCreateAndCheckoutBranch("change-a")

		testCommand({ branchCreate("change-b") }) {
			awaitFrame(
				static = "Cannot branch from change-a since it is not tracked. Please track with st branch track.",
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
	fun duplicateBranch() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		gitCreateBranch("change-a")

		testCommand({ branchCreate("change-a") }) {
			awaitFrame(
				static = "Branch change-a already exists.",
				output = "",
			)
			assertThat(awaitResult()).isFalse()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
				.containsExactly("main")
		}
	}
}
