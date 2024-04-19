package com.mattprecious.stacker.test

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import okio.Path.Companion.toPath
import kotlin.test.Test

class SimpleTest {
	@Test
	fun returnsErrorIfNoRepositoryFound() {
		stackerTest {
			// TODO: Assert against the message that's printed as well.
			assertThat(runStacker()).isEqualTo(-1)
			assertThat(fileSystem.list("/".toPath())).isEmpty()
		}
	}

	@Test
	fun environmentSetupMakesGitShaDeterministic() {
		stackerTest {
			// TODO: Introduce helpers for all of these git operations.
			environment.exec("git init")
			environment.exec("touch hello.txt")
			environment.exec("git add hello.txt")
			environment.exec("git commit -m 'Testing'")
			assertThat(environment.exec("git rev-parse HEAD"))
				.isEqualTo("f8cdffa9a5c120b21a0042138806a930e72af88f")
		}
	}
}
