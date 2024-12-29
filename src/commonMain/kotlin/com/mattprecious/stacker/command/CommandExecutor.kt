package com.mattprecious.stacker.command

import com.jakewharton.mosaic.runMosaicBlocking

interface CommandExecutor {
	fun execute(command: StackerCommand): Boolean
}

internal class RealCommandExecutor : CommandExecutor {
	override fun execute(command: StackerCommand): Boolean {
		var result = false

		runMosaicBlocking {
			command.Work(onFinish = { result = it })
		}

		return result
	}
}
