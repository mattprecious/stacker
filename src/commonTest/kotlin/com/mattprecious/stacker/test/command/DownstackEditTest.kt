package com.mattprecious.stacker.test.command

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.ui.unit.IntSize
import com.mattprecious.stacker.command.branch.branchCreate
import com.mattprecious.stacker.command.downstack.downstackEdit
import com.mattprecious.stacker.command.repo.repoInit
import com.mattprecious.stacker.delegates.Optional
import com.mattprecious.stacker.test.util.gitAdd
import com.mattprecious.stacker.test.util.gitBranches
import com.mattprecious.stacker.test.util.gitCommit
import com.mattprecious.stacker.test.util.gitCreateAndCheckoutBranch
import com.mattprecious.stacker.test.util.gitInit
import com.mattprecious.stacker.test.util.gitLog
import com.mattprecious.stacker.test.util.reset
import com.mattprecious.stacker.test.util.s
import com.mattprecious.stacker.test.util.withTestEnvironment
import okio.Path.Companion.toPath
import kotlin.test.Test

class DownstackEditTest {
	@Test
	fun errorsWhenOnTrunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main") })

		testCommand({ downstackEdit() }) {
			awaitFrame(
				static = "Not on a stack.",
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}
	}

	@Test
	fun errorsWhenOnTrailingTrunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		gitCreateAndCheckoutBranch("green-main")
		testCommand({ repoInit("main", Optional.Some("green-main")) })

		testCommand({ downstackEdit() }) {
			awaitFrame(
				static = "Not on a stack.",
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}
	}

	@Test
	fun errorsWhenOnUntrackedBranch() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main") })
		gitCreateAndCheckoutBranch("feature")

		testCommand({ downstackEdit() }) {
			awaitFrame(
				static = "Cannot edit downstack since feature is not tracked. Please track with st branch track.",
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}
	}

	@Test
	fun errorsWhenStackHasOneBranch() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main") })
		testCommand({ branchCreate("feature") })

		testCommand({ downstackEdit() }) {
			awaitFrame(
				static = "Stack only has one branch.",
				output = "",
			)

			assertThat(awaitResult()).isFalse()
		}
	}

	@Test
	fun swappingTwoBranches() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main") })
		testCommand({ branchCreate("change-a") })
		environment.exec("touch a.txt")
		gitAdd("a.txt".toPath())
		assertThat(gitCommit("Change A").short).isEqualTo("3d9b7d7")
		testCommand({ branchCreate("change-b") })
		environment.exec("touch b.txt")
		gitAdd("b.txt".toPath())
		assertThat(gitCommit("Change B").short).isEqualTo("929371e")

		testCommand({ downstackEdit() }) {
			awaitFrame(
				"""
				|❯ change-b                                $s
				|  change-a                                $s
				|  main                                    $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown", shift = true))

			awaitFrame(
				"""
				|  change-a                                $s
				|❯ change-b                                $s
				|  main                                    $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame("")

			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name to it.parent })
				.containsExactlyInAnyOrder(
					"change-a" to "change-b",
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
			"32b0f41 Change A",
			"23535fb Change B",
			"94cf6f0 Empty",
		)

		assertThat(gitBranches()).containsExactly(
			"change-a",
			"* change-b",
			"main",
		)
	}

	@Test
	fun removingBranch() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main") })
		testCommand({ branchCreate("change-a") })
		environment.exec("touch a.txt")
		gitAdd("a.txt".toPath())
		assertThat(gitCommit("Change A").short).isEqualTo("3d9b7d7")
		testCommand({ branchCreate("change-b") })
		environment.exec("touch b.txt")
		gitAdd("b.txt".toPath())
		assertThat(gitCommit("Change B").short).isEqualTo("929371e")

		testCommand({ downstackEdit() }) {
			awaitFrame(
				"""
				|❯ change-b                                $s
				|  change-a                                $s
				|  main                                    $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("Delete"))

			awaitFrame(
				"""
				|  change-b  ❯ Remove from stack, set parent to main$reset
				|❯ change-a    Untrack it                          $s
				|  main        Delete it                           $s
				|              Cancel                              $s
				|                                                  $s
				|Move branches up/down by holding Shift.           $s
				|Remove branch by pressing Backspace/Delete.       $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				"""
				|  change-b                                $s
				|❯ change-a (remove)                       $s
				|  main                                    $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))
			awaitFrame("")

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

		assertThat(gitBranches()).containsExactly(
			"change-a",
			"* change-b",
			"main",
		)
	}

	@Test
	fun untrackingBranch() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main") })
		testCommand({ branchCreate("change-a") })
		environment.exec("touch a.txt")
		gitAdd("a.txt".toPath())
		assertThat(gitCommit("Change A").short).isEqualTo("3d9b7d7")
		testCommand({ branchCreate("change-b") })
		environment.exec("touch b.txt")
		gitAdd("b.txt".toPath())
		assertThat(gitCommit("Change B").short).isEqualTo("929371e")

		testCommand({ downstackEdit() }) {
			awaitFrame(
				"""
				|❯ change-b                                $s
				|  change-a                                $s
				|  main                                    $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("Delete"))

			awaitFrame(
				"""
				|  change-b  ❯ Remove from stack, set parent to main$reset
				|❯ change-a    Untrack it                          $s
				|  main        Delete it                           $s
				|              Cancel                              $s
				|                                                  $s
				|Move branches up/down by holding Shift.           $s
				|Remove branch by pressing Backspace/Delete.       $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))

			awaitFrame(
				"""
				|  change-b    Remove from stack, set parent to main$reset
				|❯ change-a  ❯ Untrack it                          $s
				|  main        Delete it                           $s
				|              Cancel                              $s
				|                                                  $s
				|Move branches up/down by holding Shift.           $s
				|Remove branch by pressing Backspace/Delete.       $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				"""
				|  change-b                                $s
				|❯ change-a (untrack)                      $s
				|  main                                    $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))
			awaitFrame("")

			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name to it.parent })
				.containsExactlyInAnyOrder(
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

		assertThat(gitBranches()).containsExactly(
			"change-a",
			"* change-b",
			"main",
		)
	}

	@Test
	fun deletingBranch() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main") })
		testCommand({ branchCreate("change-a") })
		environment.exec("touch a.txt")
		gitAdd("a.txt".toPath())
		assertThat(gitCommit("Change A").short).isEqualTo("3d9b7d7")
		testCommand({ branchCreate("change-b") })
		environment.exec("touch b.txt")
		gitAdd("b.txt".toPath())
		assertThat(gitCommit("Change B").short).isEqualTo("929371e")

		testCommand({ downstackEdit() }) {
			awaitFrame(
				"""
				|❯ change-b                                $s
				|  change-a                                $s
				|  main                                    $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("Delete"))

			awaitFrame(
				"""
				|  change-b  ❯ Remove from stack, set parent to main$reset
				|❯ change-a    Untrack it                          $s
				|  main        Delete it                           $s
				|              Cancel                              $s
				|                                                  $s
				|Move branches up/down by holding Shift.           $s
				|Remove branch by pressing Backspace/Delete.       $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("ArrowDown"))

			awaitFrame(
				"""
				|  change-b    Remove from stack, set parent to main$reset
				|❯ change-a    Untrack it                          $s
				|  main      ❯ Delete it                           $s
				|              Cancel                              $s
				|                                                  $s
				|Move branches up/down by holding Shift.           $s
				|Remove branch by pressing Backspace/Delete.       $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				"""
				|  change-b                                $s
				|❯ change-a (delete)                       $s
				|  main                                    $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))
			awaitFrame("")

			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name to it.parent })
				.containsExactlyInAnyOrder(
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

		assertThat(gitBranches()).containsExactly(
			"* change-b",
			"main",
		)
	}

	@Test
	fun cancelling() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main") })
		testCommand({ branchCreate("change-a") })
		environment.exec("touch a.txt")
		gitAdd("a.txt".toPath())
		assertThat(gitCommit("Change A").short).isEqualTo("3d9b7d7")
		testCommand({ branchCreate("change-b") })
		environment.exec("touch b.txt")
		gitAdd("b.txt".toPath())
		assertThat(gitCommit("Change B").short).isEqualTo("929371e")

		testCommand({ downstackEdit() }) {
			awaitFrame(
				"""
				|❯ change-b                                $s
				|  change-a                                $s
				|  main                                    $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Delete"))

			awaitFrame(
				"""
				|❯ change-b  ❯ Remove from stack, set parent to main$reset
				|  change-a    Untrack it                          $s
				|  main        Delete it                           $s
				|              Cancel                              $s
				|                                                  $s
				|Move branches up/down by holding Shift.           $s
				|Remove branch by pressing Backspace/Delete.       $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("ArrowDown"))

			awaitFrame(
				"""
				|❯ change-b    Remove from stack, set parent to main$reset
				|  change-a    Untrack it                          $s
				|  main        Delete it                           $s
				|            ❯ Cancel                              $s
				|                                                  $s
				|Move branches up/down by holding Shift.           $s
				|Remove branch by pressing Backspace/Delete.       $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				"""
				|❯ change-b                                $s
				|  change-a                                $s
				|  main                                    $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))
			awaitFrame("")

			assertThat(awaitResult()).isTrue()
		}

		withDatabase {
			assertThat(it.branchQueries.selectAll().executeAsList().map { it.name to it.parent })
				.containsExactlyInAnyOrder(
					"change-b" to "change-a",
					"change-a" to "main",
					"main" to null,
				)
		}

		assertThat(gitLog("main")).containsExactly(
			"94cf6f0 Empty",
		)

		assertThat(gitLog("change-b")).containsExactly(
			"929371e Change B",
			"3d9b7d7 Change A",
			"94cf6f0 Empty",
		)

		assertThat(gitLog("change-a")).containsExactly(
			"3d9b7d7 Change A",
			"94cf6f0 Empty",
		)

		assertThat(gitBranches()).containsExactly(
			"change-a",
			"* change-b",
			"main",
		)
	}

	@Test
	fun removingWithNarrowTerminal() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main") })
		testCommand({ branchCreate("change-a") })
		assertThat(gitCommit("Change A"))
		testCommand({ branchCreate("change-b") })
		assertThat(gitCommit("Change B"))

		testCommand({ downstackEdit() }) {
			setSize(IntSize(45, 10))

			awaitFrame(
				"""
				|❯ change-b                                $s
				|  change-a                                $s
				|  main                                    $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Delete"))

			awaitFrame(
				"""
				|What would you like to do with change-b?  $s
				|❯ Remove from stack, set parent to main   $s
				|  Untrack it                              $s
				|  Delete it                               $s
				|  Cancel                                  $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				"""
				|❯ change-b (remove)                       $s
				|  change-a                                $s
				|  main                                    $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)
		}
	}

	@Test
	fun deletePromptMovesWhenTerminalSizeChanges() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")
		testCommand({ repoInit("main") })
		testCommand({ branchCreate("change-a") })
		assertThat(gitCommit("Change A"))
		testCommand({ branchCreate("change-b") })
		assertThat(gitCommit("Change B"))

		testCommand({ downstackEdit() }) {
			awaitFrame(
				"""
				|❯ change-b                                $s
				|  change-a                                $s
				|  main                                    $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Delete"))

			awaitFrame(
				"""
				|❯ change-b  ❯ Remove from stack, set parent to main$reset
				|  change-a    Untrack it                          $s
				|  main        Delete it                           $s
				|              Cancel                              $s
				|                                                  $s
				|Move branches up/down by holding Shift.           $s
				|Remove branch by pressing Backspace/Delete.       $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))

			awaitFrame(
				"""
				|❯ change-b    Remove from stack, set parent to main$reset
				|  change-a  ❯ Untrack it                          $s
				|  main        Delete it                           $s
				|              Cancel                              $s
				|                                                  $s
				|Move branches up/down by holding Shift.           $s
				|Remove branch by pressing Backspace/Delete.       $s
				""".trimMargin(),
			)

			setSize(IntSize(60, 10))

			awaitFrame(
				"""
				|What would you like to do with change-b?  $s
				|  Remove from stack, set parent to main   $s
				|❯ Untrack it                              $s
				|  Delete it                               $s
				|  Cancel                                  $s
				|                                          $s
				|Move branches up/down by holding Shift.   $s
				|Remove branch by pressing Backspace/Delete.$reset
				""".trimMargin(),
			)

			setSize(IntSize(61, 10))

			awaitFrame(
				"""
				|❯ change-b    Remove from stack, set parent to main$reset
				|  change-a  ❯ Untrack it                          $s
				|  main        Delete it                           $s
				|              Cancel                              $s
				|                                                  $s
				|Move branches up/down by holding Shift.           $s
				|Remove branch by pressing Backspace/Delete.       $s
				""".trimMargin(),
			)
		}
	}
}
