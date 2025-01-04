package com.mattprecious.stacker.test.command

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.turbine.Turbine
import app.cash.turbine.plusAssign
import com.jakewharton.mosaic.testing.runMosaicTest
import com.mattprecious.stacker.command.CommandExecutor
import com.mattprecious.stacker.command.StackerCommand

class TestCommandExecutor : CommandExecutor {
	val outputs = Turbine<String>()

	override suspend fun execute(command: StackerCommand): Boolean {
		var result by mutableStateOf<Boolean?>(null)

		runMosaicTest {
			setContent {
				command.Work(onFinish = { result = it })
			}

			// The first snapshot will always be empty since command execution is started inside a
			// launched effect.
			awaitSnapshot()

			while (result == null) {
				outputs += awaitSnapshot()
			}
		}

		return result!!
	}
}
