package com.mattprecious.stacker.command

import com.jakewharton.mosaic.runMosaic

interface CommandExecutor {
	suspend fun execute(command: StackerCommand): Boolean
}

internal class RealCommandExecutor : CommandExecutor {
	override suspend fun execute(command: StackerCommand): Boolean {
		var result = false

		runMosaic {
			command.Work(onFinish = { result = it })
		}

		return result
	}
}
