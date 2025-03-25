package com.mattprecious.stacker.test.command

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.jakewharton.mosaic.terminal.KeyboardEvent
import com.mattprecious.stacker.command.branch.branchCreate
import com.mattprecious.stacker.command.repo.repoInit
import com.mattprecious.stacker.command.upstack.upstackOnto
import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.delegates.Optional
import com.mattprecious.stacker.test.util.Enter
import com.mattprecious.stacker.test.util.gitAdd
import com.mattprecious.stacker.test.util.gitCheckoutBranch
import com.mattprecious.stacker.test.util.gitCommit
import com.mattprecious.stacker.test.util.gitCreateAndCheckoutBranch
import com.mattprecious.stacker.test.util.gitInit
import com.mattprecious.stacker.test.util.gitLog
import com.mattprecious.stacker.test.util.withTestEnvironment
import okio.Path.Companion.toPath
import kotlin.test.Test

class UpstackOntoTest {
	@Test
	fun untracked() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		gitCreateAndCheckoutBranch("change-a")

		testCommand({ upstackOnto() }) {
			awaitFrame(
				static = "Cannot retarget change-a since it is not tracked. Please track with st branch track.",
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}
	}

	@Test
	fun trunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })

		testCommand({ upstackOnto() }) {
			awaitFrame(
				static = "Cannot retarget a trunk branch.",
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
		testCommand({ repoInit("main", Optional.Some("green-main")) })

		testCommand({ upstackOnto() }) {
			awaitFrame(
				static = "Cannot retarget a trunk branch.",
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}
	}

	@Test
	fun single() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("change-a") })
		environment.exec("touch a.txt")
		gitAdd("a.txt".toPath())
		assertThat(gitCommit("Change A").short).isEqualTo("3d9b7d7")

		testCommand({ upstackOnto() }) {
			awaitFrame(
				"""
				|Select the parent branch for change-a:
				|❯ ○ main
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Select the parent branch for change-a: main",
				output = "",
			)

			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name to it.parent })
				.containsExactlyInAnyOrder(
					"change-a" to "main",
					"main" to null,
				)
		}

		assertThat(gitLog("main")).containsExactly(
			"94cf6f0 Empty",
		)

		assertThat(gitLog("change-a")).containsExactly(
			"3d9b7d7 Change A",
			"94cf6f0 Empty",
		)
	}

	@Test
	fun linearToFork() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("change-a") })
		environment.exec("touch a.txt")
		gitAdd("a.txt".toPath())
		assertThat(gitCommit("Change A").short).isEqualTo("3d9b7d7")
		testCommand({ branchCreate("change-b") })
		environment.exec("touch b.txt")
		gitAdd("b.txt".toPath())
		assertThat(gitCommit("Change B").short).isEqualTo("929371e")

		testCommand({ upstackOnto() }) {
			awaitFrame(
				"""
				|Select the parent branch for change-b:
				|❯ ○ change-a
				|  ○ main
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Select the parent branch for change-b: main",
				output = "",
			)

			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name to it.parent })
				.containsExactlyInAnyOrder(
					"change-a" to "main",
					"change-b" to "main",
					"main" to null,
				)
		}

		assertThat(gitLog("main")).containsExactly(
			"94cf6f0 Empty",
		)

		assertThat(gitLog("change-b")).containsExactly(
			"23535fb Change B",
			"94cf6f0 Empty",
		)

		assertThat(gitLog("change-a")).containsExactly(
			"3d9b7d7 Change A",
			"94cf6f0 Empty",
		)
	}

	@Test
	fun withChildren() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main", Optional.None) })
		testCommand({ branchCreate("change-a") })
		environment.exec("touch a.txt")
		gitAdd("a.txt".toPath())
		assertThat(gitCommit("Change A").short).isEqualTo("3d9b7d7")
		testCommand({ branchCreate("change-b") })
		environment.exec("touch b.txt")
		gitAdd("b.txt".toPath())
		assertThat(gitCommit("Change B").short).isEqualTo("929371e")
		testCommand({ branchCreate("change-c") })
		environment.exec("touch c.txt")
		gitAdd("c.txt".toPath())
		assertThat(gitCommit("Change C").short).isEqualTo("bd5a1ab")
		gitCheckoutBranch("change-b")

		testCommand({ upstackOnto() }) {
			awaitFrame(
				"""
				|Select the parent branch for change-b:
				|❯ ○ change-a
				|  ○ main
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			awaitFrame(
				static = "Select the parent branch for change-b: main",
				output = "",
			)

			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList())
				.containsExactlyInAnyOrder(
					Branch(
						name = "change-c",
						parent = "change-b",
						parentSha = "23535fb16a271a6a7e8684c80ba6ebf7985c6774",
						prNumber = null,
						hasAskedToDelete = false,
					),
					Branch(
						name = "change-b",
						parent = "main",
						parentSha = "94cf6f02cfb0fe7422f23292205b4485fb83ae2e",
						prNumber = null,
						hasAskedToDelete = false,
					),
					Branch(
						name = "change-a",
						parent = "main",
						parentSha = "94cf6f02cfb0fe7422f23292205b4485fb83ae2e",
						prNumber = null,
						hasAskedToDelete = false,
					),
					Branch(
						name = "main",
						parent = null,
						parentSha = null,
						prNumber = null,
						hasAskedToDelete = false,
					),
				)
		}

		assertThat(gitLog("main")).containsExactly(
			"94cf6f0 Empty",
		)

		assertThat(gitLog("change-a")).containsExactly(
			"3d9b7d7 Change A",
			"94cf6f0 Empty",
		)

		assertThat(gitLog("change-b")).containsExactly(
			"23535fb Change B",
			"94cf6f0 Empty",
		)

		assertThat(gitLog("change-c")).containsExactly(
			"ecaa71f Change C",
			"23535fb Change B",
			"94cf6f0 Empty",
		)
	}
}
