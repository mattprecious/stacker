package com.mattprecious.stacker

import com.mattprecious.stacker.command.Stacker
import com.mattprecious.stacker.config.RealConfigManager
import com.mattprecious.stacker.lock.RealLocker
import com.mattprecious.stacker.remote.GitHubRemote
import com.mattprecious.stacker.shell.RealShell
import com.mattprecious.stacker.stack.RealStackManager
import com.mattprecious.stacker.vc.GitVersionControl
import java.lang.foreign.Arena
import kotlin.io.path.div

fun main(args: Array<String>) {
	Arena.openConfined().use { arena ->
		val shell = RealShell()
		GitVersionControl(arena, shell).use { vc ->

			val dbPath = vc.configDirectory / "stacker.db"
			withDatabase(dbPath.toString()) { db ->
				val stackManager = RealStackManager(db)
				val configManager = RealConfigManager(db, stackManager)
				val locker = RealLocker(db, stackManager, vc)
				val remote = GitHubRemote(vc.originUrl, configManager)

				Stacker(
					configManager = configManager,
					locker = locker,
					remote = remote,
					stackManager = stackManager,
					vc = vc,
				).main(args)
			}
		}
	}
}
