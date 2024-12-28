package com.mattprecious.stacker.command.branch

import com.mattprecious.stacker.command.StackerCliktCommand
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Down(
	configManager: ConfigManager,
	locker: Locker,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCliktCommand(shortAlias = "d") {
	override val command by lazy {
		DownCommand(
			configManager = configManager,
			locker = locker,
			stackManager = stackManager,
			vc = vc,
		)
	}
}

internal class DownCommand(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val parent = stackManager.getBranch(vc.currentBranchName)!!.parent
		if (parent == null) {
			printStaticError("Already at the base.")
			abort()
		}

		vc.checkout(parent.name)
	}
}
