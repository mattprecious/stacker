package com.mattprecious.stacker.test

import assertk.assertFailure
import assertk.assertions.isInstanceOf
import com.github.ajalt.clikt.core.PrintHelpMessage
import org.junit.Test

class SimpleTest {
	@Test fun emptyArgsPrintsHelpMessage() {
		stackerTest {
			assertFailure { run() }.isInstanceOf(PrintHelpMessage::class)
		}
	}
}
