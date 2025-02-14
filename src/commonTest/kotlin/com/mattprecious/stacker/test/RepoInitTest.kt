package com.mattprecious.stacker.test

import assertk.assertThat
import assertk.assertions.isFalse
import com.mattprecious.stacker.command.repo.repoInit
import kotlin.test.Test

class RepoInitTest {
	@Test
	fun errorsInitializingEmptyRepo() = withTestEnvironment {
		gitInit()

		testCommand({ repoInit() }) {
			awaitFrame(
				static = "Stacker cannot be initialized in a completely empty repository. " +
					"Please make a commit first.",
				output = "",
			)
			assertThat(awaitResult()).isFalse()
		}
	}
}
