package com.mattprecious.stacker.test

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.jakewharton.mosaic.layout.KeyEvent
import com.mattprecious.stacker.command.repo.repoInit
import kotlin.test.Test

class RepoTest {
	@Test
	fun errorsInitializingEmptyRepo() = withTestEnvironment {
		gitInit()

		testCommand({ repoInit() }) {
			awaitFrame(
				static = "Stacker cannot be initialized in a completely empty repository. " +
					"Please make a commit first.",
				output = "",
			)
			assertThat(result).isNotNull().isFalse()
		}
	}

	@Test
	fun successfullyInitsWithNoTrailingTrunk() = withTestEnvironment {
		gitInit()
		gitCommit("Empty")

		testCommand({ repoInit() }) {
			awaitFrame(
				"""
				|Select your trunk branch, which you open pull requests against:$s
				|❯ main                                                         $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Select your trunk branch, which you open pull requests against: main",
				output = "Do you use a trailing-trunk workflow? [y/N]: ",
			)

			sendText("n")

			awaitFrame("Do you use a trailing-trunk workflow? [y/N]: n")

			sendKeyEvent(KeyEvent("Enter"))

			awaitFrame(
				static = "Do you use a trailing-trunk workflow? [y/N]: n",
				output = "",
			)

			assertThat(result).isNotNull().isTrue()
		}
	}
}
