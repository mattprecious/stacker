package com.mattprecious.stacker.test

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import kotlin.test.Test

class RepoTest {
	@Test
	fun errorsInitializingEmptyRepo() = stackerTest {
		gitInit()
		withStacker {
			assertThat(it.repoInit()).isFalse()
			assertThat(commandExecutor.outputs.awaitItem())
				.isEqualTo(
					"Stacker cannot be initialized in a completely empty repository. " +
						"Please make a commit, first.\n",
				)
		}
	}
}
