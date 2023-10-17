package com.mattprecious.stacker

import com.mattprecious.stacker.command.Stacker
import com.mattprecious.stacker.config.RealConfigManager
import com.mattprecious.stacker.lock.RealLocker
import com.mattprecious.stacker.remote.GitHubRemote
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.shell.RealShell
import com.mattprecious.stacker.stack.RealStackManager
import com.mattprecious.stacker.vc.GitVersionControl
import java.lang.foreign.Arena
import kotlin.io.path.div
import kotlin.system.exitProcess

fun main(args: Array<String>) {
	withStacker {
		main(args)
	}
}

internal fun withStacker(
	remoteOverride: Remote? = null,
	block: (Stacker) -> Unit,
) {
	Arena.ofConfined().use { arena ->
		val shell = RealShell()
		GitVersionControl(arena, shell).use { vc ->

			if (!vc.repoDiscovered) {
				println("No repository found at ${System.getProperty("user.dir")}.")
				exitProcess(-1)
			}

			val dbPath = vc.configDirectory / "stacker.db"
			withDatabase(dbPath.toString()) { db ->
				val stackManager = RealStackManager(db)
				val configManager = RealConfigManager(db, stackManager)
				val locker = RealLocker(db, stackManager, vc)
				val remote = remoteOverride ?: GitHubRemote(vc.originUrl, configManager)

				Stacker(
					configManager = configManager,
					locker = locker,
					remote = remote,
					stackManager = stackManager,
					vc = vc,
				).let(block)
			}
		}
	}
}
