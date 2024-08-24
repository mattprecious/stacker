package com.mattprecious.stacker.test

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotZero
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
}
