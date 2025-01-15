package com.mattprecious.stacker.test

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.mattprecious.stacker.command.repo.repoInit
import kotlin.test.Test

class RepoTest {
	@Test
	fun errorsInitializingEmptyRepo() = stackerTest {
		gitInit()

		testCommand({ repoInit() }) {
			assertThat(awaitResult()).isFalse()
			assertThat(awaitOutput()).isEmpty()
			assertThat(awaitStatic())
				.isEqualTo(
					"Stacker cannot be initialized in a completely empty repository. " +
						"Please make a commit first.",
				)
		}
	}
}
