package com.mattprecious.stacker.test.util

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import assertk.assertions.message
import com.mattprecious.stacker.RepoNotFoundException
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertFailsWith

class EnvironmentTest {
	@Test
	fun throwsErrorIfNoRepositoryFound() = withTestEnvironment {
		val t = assertFailsWith<RepoNotFoundException> { testInit() }
		assertThat(t).message()
			.isEqualTo("No repository found at ${fileSystem.canonicalize(".".toPath())}.")

		assertThat(fileSystem.list(".".toPath())).isEmpty()
	}

	@Test
	fun environmentSetupMakesGitShaDeterministic() = withTestEnvironment {
		gitInit()
		environment.exec("touch hello.txt")
		gitAdd("hello.txt".toPath())
		gitCommit("Testing")
		assertThat(gitSha().long).isEqualTo("f8cdffa9a5c120b21a0042138806a930e72af88f")
	}

	@Test
	fun dbIsCreatedInGitDirectory() = withTestEnvironment {
		gitInit()
		assertThat(fileSystem.exists(defaultDbPath)).isFalse()

		testInit()
		assertThat(fileSystem.exists(defaultDbPath)).isTrue()
	}
}
