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
			assertThat(run()).isEqualTo(-1)
			assertThat(fileSystem.list("/".toPath())).isEmpty()
		}
	}
}
