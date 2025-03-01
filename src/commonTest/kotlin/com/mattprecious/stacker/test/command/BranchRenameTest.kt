package com.mattprecious.stacker.test.command

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mattprecious.stacker.command.branch.branchCreate
import com.mattprecious.stacker.command.branch.branchRename
import com.mattprecious.stacker.command.repo.repoInit
import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.delegates.Optional
import com.mattprecious.stacker.test.util.gitBranches
import com.mattprecious.stacker.test.util.gitCheckoutBranch
import com.mattprecious.stacker.test.util.gitCommit
import com.mattprecious.stacker.test.util.gitCreateAndCheckoutBranch
import com.mattprecious.stacker.test.util.gitInit
import com.mattprecious.stacker.test.util.withTestEnvironment
import kotlin.test.Test

class BranchRenameTest {
	@Test
	fun renameLeaf() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("change-a") })

		testCommand({ branchRename("new-a") }) {
			awaitFrame("")
			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
				.containsExactly("main", "new-a")
		}

		assertThat(gitBranches()).containsExactly(
			"main",
			"* new-a",
		)
	}

	@Test
	fun renameWithChildren() = withTestEnvironment {
		gitInit()
		val sha = gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("change-a") })
		testCommand({ branchCreate("change-b") })
		testCommand({ branchCreate("change-c") })
		gitCheckoutBranch("change-a")
		testCommand({ branchCreate("change-d") })
		gitCheckoutBranch("change-a")

		testCommand({ branchRename("new-a") }) {
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
					name = "new-a",
					parent = "main",
					parentSha = sha.long,
					prNumber = null,
					hasAskedToDelete = false,
				),
				Branch(
					name = "change-b",
					parent = "new-a",
					parentSha = sha.long,
					prNumber = null,
					hasAskedToDelete = false,
				),
				Branch(
					name = "change-c",
					parent = "change-b",
					parentSha = sha.long,
					prNumber = null,
					hasAskedToDelete = false,
				),
				Branch(
					name = "change-d",
					parent = "new-a",
					parentSha = sha.long,
					prNumber = null,
					hasAskedToDelete = false,
				),
			)
		}

		assertThat(gitBranches()).containsExactly(
			"change-b",
			"change-c",
			"change-d",
			"main",
			"* new-a",
		)
	}

	@Test
	fun cannotRenameTrunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })

		testCommand({ branchRename("trunk") }) {
			awaitFrame(
				static = "Cannot rename a trunk branch.",
				output = "",
			)
			assertThat(awaitResult()).isFalse()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
				.containsExactly("main")
		}

		assertThat(gitBranches()).containsExactly("* main")
	}

	@Test
	fun cannotRenameTrailingTrunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		gitCreateAndCheckoutBranch("green-main")
		testCommand({ repoInit("main", Optional.Some("green-main")) })

		testCommand({ branchRename("green-trunk") }) {
			awaitFrame(
				static = "Cannot rename a trunk branch.",
				output = "",
			)
			assertThat(awaitResult()).isFalse()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
				.containsExactlyInAnyOrder("main", "green-main")
		}

		assertThat(gitBranches()).containsExactly(
			"* green-main",
			"main",
		)
	}
}
