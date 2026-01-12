package com.mattprecious.stacker.test.command

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isTrue
import com.jakewharton.mosaic.layout.KeyEvent
import com.mattprecious.stacker.command.branch.branchTrack
import com.mattprecious.stacker.command.repo.repoInit
import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.delegates.Optional
import com.mattprecious.stacker.test.util.gitCheckoutBranch
import com.mattprecious.stacker.test.util.gitCommit
import com.mattprecious.stacker.test.util.gitCreateAndCheckoutBranch
import com.mattprecious.stacker.test.util.gitInit
import com.mattprecious.stacker.test.util.s
import com.mattprecious.stacker.test.util.withTestEnvironment
import kotlin.test.Test

class BranchTrackTest {
  @Test
  fun alreadyTracked() = withTestEnvironment {
    gitInit()
    gitCommit("Empty")
    testCommand({ repoInit("main", Optional.None) })

    testCommand({ branchTrack(null) }) {
      awaitFrame(static = "Branch main is already tracked.", output = "")

      assertThat(awaitResult()).isTrue()
    }

    testCommand({ branchTrack("main") }) {
      awaitFrame(static = "Branch main is already tracked.", output = "")

      assertThat(awaitResult()).isTrue()
    }

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList())
        .containsExactly(
          Branch(
            name = "main",
            parent = null,
            parentSha = null,
            prNumber = null,
            hasAskedToDelete = false,
          )
        )
    }
  }

  @Test
  fun withOnlyTrunkAsAncestor() = withTestEnvironment {
    gitInit()
    val mainSha = gitCommit("Empty")
    testCommand({ repoInit("main", Optional.None) })
    gitCreateAndCheckoutBranch("change-a")
    gitCommit("Change A")

    testCommand({ branchTrack(null) }) {
      awaitFrame("")
      assertThat(awaitResult()).isTrue()
    }

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList())
        .containsExactly(
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
  fun multipleAncestors() = withTestEnvironment {
    gitInit()
    val mainSha = gitCommit("Empty")
    testCommand({ repoInit("main", Optional.None) })
    gitCreateAndCheckoutBranch("change-a")
    val parentSha = gitCommit("Change A")
    testCommand({ branchTrack(null) })
    gitCreateAndCheckoutBranch("change-b")
    gitCommit("Change B")

    testCommand({ branchTrack(null) }) {
      awaitFrame(
        """
				|Choose a parent branch for change-b:$s
				|  ○ change-a                        $s
				|❯ ○ main                            $s
				"""
          .trimMargin()
      )

      sendKeyEvent(KeyEvent("ArrowUp"))
      sendKeyEvent(KeyEvent("Enter"))

      awaitFrame(static = "Choose a parent branch for change-b: change-a", output = "")

      assertThat(awaitResult()).isTrue()
    }

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList())
        .containsExactly(
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
  fun nonAncestorsAreFilteredOut() = withTestEnvironment {
    gitInit()
    val mainSha = gitCommit("Empty")
    testCommand({ repoInit("main", Optional.None) })
    gitCreateAndCheckoutBranch("change-a")
    gitCommit("Change A")
    testCommand({ branchTrack(null) })
    gitCheckoutBranch("main")
    gitCreateAndCheckoutBranch("change-b")
    gitCommit("Change B")

    testCommand({ branchTrack(null) }) {
      awaitFrame("")
      assertThat(awaitResult()).isTrue()
    }

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList())
        .containsExactly(
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
            parent = "main",
            parentSha = mainSha.long,
            prNumber = null,
            hasAskedToDelete = false,
          ),
        )
    }
  }

  @Test
  fun trunkIsAlwaysIncluded() = withTestEnvironment {
    gitInit()
    val mainSha = gitCommit("Empty")
    gitCreateAndCheckoutBranch("change-a")
    gitCommit("Change A")
    gitCheckoutBranch("main")
    gitCommit("Main update")
    gitCreateAndCheckoutBranch("green-main")
    testCommand({ repoInit("main", Optional.Some("green-main")) })

    testCommand({ branchTrack("change-a") }) {
      awaitFrame(
        """
				|Choose a parent branch for change-a:$s
				|❯ ○ green-main                      $s
				|  ○ main                            $s
				"""
          .trimMargin()
      )
    }
  }
}
