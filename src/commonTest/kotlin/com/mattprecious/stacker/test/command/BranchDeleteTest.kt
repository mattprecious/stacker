package com.mattprecious.stacker.test.command

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mattprecious.stacker.command.branch.branchCreate
import com.mattprecious.stacker.command.branch.branchDelete
import com.mattprecious.stacker.command.repo.repoInit
import com.mattprecious.stacker.delegates.Optional
import com.mattprecious.stacker.test.util.gitCheckoutBranch
import com.mattprecious.stacker.test.util.gitCommit
import com.mattprecious.stacker.test.util.gitCreateAndCheckoutBranch
import com.mattprecious.stacker.test.util.gitCreateBranch
import com.mattprecious.stacker.test.util.gitCurrentBranch
import com.mattprecious.stacker.test.util.gitInit
import com.mattprecious.stacker.test.util.withTestEnvironment
import kotlin.test.Test

class BranchDeleteTest {
  @Test
  fun cannotDeleteTrunk() = withTestEnvironment {
    gitInit()
    gitCommit("Empty")
    testCommand({ repoInit("main", Optional.None) })

    testCommand({ branchDelete("main") }) {
      awaitFrame(static = "Cannot delete a trunk branch.", output = "")
      assertThat(awaitResult()).isFalse()
    }

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
        .containsExactly("main")
    }
  }

  @Test
  fun cannotDeleteTrailingTrunk() = withTestEnvironment {
    gitInit()
    gitCommit("Empty")
    gitCreateAndCheckoutBranch("green-main")
    testCommand({ repoInit("main", Optional.Some("green-main")) })

    testCommand({ branchDelete("green-main") }) {
      awaitFrame(static = "Cannot delete a trunk branch.", output = "")
      assertThat(awaitResult()).isFalse()
    }

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
        .containsExactly("main", "green-main")
    }
  }

  @Test
  fun branchWithChildren() = withTestEnvironment {
    gitInit()
    gitCommit("Empty")
    testCommand({ repoInit("main", Optional.None) })
    testCommand({ branchCreate("change-a") })
    testCommand({ branchCreate("change-b") })

    testCommand({ branchDelete("change-a") }) {
      awaitFrame(static = "Branch has children. Please retarget or untrack them.", output = "")
      assertThat(awaitResult()).isFalse()
    }

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
        .containsExactly("main", "change-a", "change-b")
    }
  }

  @Test
  fun currentBranch() = withTestEnvironment {
    gitInit()
    gitCommit("Empty")
    testCommand({ repoInit("main", Optional.None) })
    testCommand({ branchCreate("change-a") })
    testCommand({ branchCreate("change-b") })

    testCommand({ branchDelete(null) }) {
      awaitFrame("")
      assertThat(awaitResult()).isTrue()
    }

    assertThat(gitCurrentBranch()).isEqualTo("change-a")

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
        .containsExactly("main", "change-a")
    }

    testCommand({ branchDelete(null) }) {
      awaitFrame("")
      assertThat(awaitResult()).isTrue()
    }

    assertThat(gitCurrentBranch()).isEqualTo("main")

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
        .containsExactly("main")
    }
  }

  @Test
  fun currentBranchUntracked() = withTestEnvironment {
    gitInit()
    gitCommit("Empty")
    testCommand({ repoInit("main", Optional.None) })
    gitCreateAndCheckoutBranch("change-a")

    testCommand({ branchDelete(null) }) {
      awaitFrame("")
      assertThat(awaitResult()).isTrue()
    }

    assertThat(gitCurrentBranch()).isEqualTo("main")

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
        .containsExactly("main")
    }
  }

  @Test
  fun currentBranchUntrackedWithTrailingTrunk() = withTestEnvironment {
    gitInit()
    gitCommit("Empty")
    gitCreateAndCheckoutBranch("green-main")
    testCommand({ repoInit("main", Optional.Some("green-main")) })
    gitCreateAndCheckoutBranch("change-a")

    testCommand({ branchDelete(null) }) {
      awaitFrame("")
      assertThat(awaitResult()).isTrue()
    }

    assertThat(gitCurrentBranch()).isEqualTo("green-main")

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
        .containsExactly("main", "green-main")
    }
  }

  @Test
  fun notCurrentBranch() = withTestEnvironment {
    gitInit()
    gitCommit("Empty")
    testCommand({ repoInit("main", Optional.None) })
    testCommand({ branchCreate("change-a") })
    testCommand({ branchCreate("change-b") })
    gitCheckoutBranch("main")

    testCommand({ branchDelete("change-b") }) {
      awaitFrame("")
      assertThat(awaitResult()).isTrue()
    }

    assertThat(gitCurrentBranch()).isEqualTo("main")

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
        .containsExactly("main", "change-a")
    }

    testCommand({ branchDelete("change-a") }) {
      awaitFrame("")
      assertThat(awaitResult()).isTrue()
    }

    assertThat(gitCurrentBranch()).isEqualTo("main")

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
        .containsExactly("main")
    }
  }

  @Test
  fun notCurrentBranchUntracked() = withTestEnvironment {
    gitInit()
    gitCommit("Empty")
    testCommand({ repoInit("main", Optional.None) })
    testCommand({ branchCreate("change-a") })
    gitCreateBranch("change-b")

    testCommand({ branchDelete("change-b") }) {
      awaitFrame("")
      assertThat(awaitResult()).isTrue()
    }

    assertThat(gitCurrentBranch()).isEqualTo("change-a")

    withDatabase {
      assertThat(it.branchQueries.selectAll().executeAsList().map { it.name })
        .containsExactly("main", "change-a")
    }
  }
}
