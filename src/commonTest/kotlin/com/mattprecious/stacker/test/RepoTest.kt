package com.mattprecious.stacker.test

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotZero
import assertk.assertions.isZero
import kotlin.test.Test

class RepoTest {
	@Test
	fun errorsInitializingEmptyRepo() = stackerTest {
		gitInit()
		with(runStacker("repo", "init")) {
			assertThat(statusCode).isNotZero()
			assertThat(output)
				.isEqualTo(
					"Stacker cannot be initialized in a completely empty repository. " +
						"Please make a commit, first.\n",
				)
		}
	}

	@Test
	fun picksOnlyBranchAsTrunk() = stackerTest {
		gitInit()
		gitCommit("Initial commit")

		with(runStacker("repo", "init", stdin = "n\n")) {
			assertThat(statusCode).isZero()
			assertThat(output)
				.isEqualTo("Do you use a trailing-trunk workflow? [y/N]: ")
		}
	}

	@Test
	fun defaultsToConfigDefaultIfPresent() = stackerTest {
		gitInit()
		gitCommit("Initial commit")
		gitCreateAndSwitchToBranch("main2")

		with(runStacker("repo", "init", stdin = "n\n")) {
			assertThat(statusCode).isZero()
			assertThat(output)
				.isEqualTo("Do you use a trailing-trunk workflow? [y/N]: ")
		}
	}
}
