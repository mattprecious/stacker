package com.mattprecious.stacker.command.rebase

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.error
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.perform
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Rebase(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	private val abort: Boolean by option().flag()
	private val cont by option("--continue").flag()

	override fun run() {
		requireInitialized(configManager)

		when {
			abort -> abortOperation()
			cont -> continueOperation()
			else -> throw PrintHelpMessage(currentContext, error = true)
		}
	}

	private fun abortOperation() {
		if (!locker.hasLock()) {
			error("Nothing to abort.")
			throw Abort()
		}

		locker.cancelOperation { operation ->
			when (operation) {
				is Locker.Operation.Restack -> {
					vc.abortRebase()
					vc.checkout(operation.startingBranch)
				}
			}
		}
	}

	private fun continueOperation() {
		if (!locker.hasLock()) {
			error("Nothing to continue.")
			throw Abort()
		}

		locker.continueOperation { operation ->
			when (operation) {
				is Locker.Operation.Restack -> {
					if (vc.continueRebase(operation.branches.first())) {
						operation.perform(stackManager, vc, continuing = true)
					}
				}
			}
		}
	}
}
