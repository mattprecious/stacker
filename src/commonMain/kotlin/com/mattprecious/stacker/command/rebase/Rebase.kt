package com.mattprecious.stacker.command.rebase

import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mattprecious.stacker.command.StackerCliktCommand
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.perform
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Rebase(
	configManager: ConfigManager,
	locker: Locker,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCliktCommand() {
	private val abort: Boolean by option().flag()
	private val cont: Boolean by option("--continue").flag()

	override val command by lazy {
		when {
			abort -> RebaseAbortCommand(
				configManager = configManager,
				locker = locker,
				vc = vc,
			)

			cont -> RebaseContinueCommand(
				configManager = configManager,
				locker = locker,
				stackManager = stackManager,
				vc = vc,
			)
			// TODO
			else -> throw PrintHelpMessage(currentContext, error = true)
		}
	}
}

internal class RebaseAbortCommand(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)

		if (!locker.hasLock()) {
			printStaticError("Nothing to abort.")
			abort()
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
}

internal class RebaseContinueCommand(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)

		if (!locker.hasLock()) {
			printStaticError("Nothing to continue.")
			abort()
		}

		locker.continueOperation { operation ->
			when (operation) {
				is Locker.Operation.Restack -> {
					if (vc.continueRebase(operation.branches.first())) {
						operation.perform(this@work, this, stackManager, vc, continuing = true)
					}
				}
			}
		}
	}
}
