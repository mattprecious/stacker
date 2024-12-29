package com.mattprecious.stacker.test.command

import app.cash.turbine.Turbine
import com.jakewharton.mosaic.renderMosaic
import com.mattprecious.stacker.command.CommandExecutor
import com.mattprecious.stacker.command.StackerCommand

class TestCommandExecutor : CommandExecutor {
	val outputs = Turbine<String>()

	override fun execute(command: StackerCommand): Boolean {
		var result = false

		val output = renderMosaic {
			command.Work(onFinish = { result = it })
		}

		outputs.add(output)

		return result
	}
}
