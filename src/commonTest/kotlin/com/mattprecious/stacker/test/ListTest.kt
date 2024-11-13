package com.mattprecious.stacker.test

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ListTest {
	@Test
	fun emptyRepoShowsMainBranch() = stackerTest {
		gitInit()
		with(runStacker("ls")) {
			assertThat(output).isEqualTo("◉ main")
		}
	}
}
