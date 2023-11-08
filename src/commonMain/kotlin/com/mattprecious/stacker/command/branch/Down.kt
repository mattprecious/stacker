package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.core.Abort
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Down(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "d") {
	override fun run() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val parent = stackManager.getBranch(vc.currentBranchName)!!.parent
		if (parent == null) {
			echo("Already at the base.", err = true)
			throw Abort()
		}

		vc.checkout(parent.name)
	}
}
