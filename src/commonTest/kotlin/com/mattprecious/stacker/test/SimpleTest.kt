package com.mattprecious.stacker.test

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import assertk.assertions.isZero
import assertk.assertions.message
import com.mattprecious.stacker.RepoNotFoundException
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SimpleTest {
	@Test
	fun throwsErrorIfNoRepositoryFound() {
		stackerTest {
			val t = assertFailsWith<RepoNotFoundException> { runStacker() }
			assertThat(t).message()
				.isEqualTo("No repository found at ${fileSystem.canonicalize(".".toPath())}.")

			assertThat(fileSystem.list(".".toPath())).isEmpty()
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

	@Test
	fun dbIsCreatedInGitDirectory() {
		stackerTest {
			environment.exec("git init")
			assertThat(fileSystem.exists(".git/stacker.db".toPath())).isFalse()

			with(runStacker()) {
				assertThat(statusCode).isZero()
				assertThat(fileSystem.exists(".git/stacker.db".toPath())).isTrue()
			}
		}
	}
}
