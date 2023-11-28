package com.mattprecious.stacker

import com.mattprecious.stacker.command.Stacker
import com.mattprecious.stacker.config.RealConfigManager
import com.mattprecious.stacker.lock.RealLocker
import com.mattprecious.stacker.remote.GitHubRemote
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.shell.RealShell
import com.mattprecious.stacker.stack.RealStackManager
import com.mattprecious.stacker.vc.GitVersionControl
import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.cinterop.memScoped
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.system.exitProcess

fun main(args: Array<String>) {
	withStacker {
		it.main(args)
	}
}

@OptIn(ExperimentalSerializationApi::class)
internal fun withStacker(
	remoteOverride: Remote? = null,
	block: (Stacker) -> Unit,
) {
	memScoped {
		val fs = FileSystem.SYSTEM
		val shell = RealShell()
		GitVersionControl(this, fs, shell).use { vc ->
			if (!vc.repoDiscovered) {
				println("No repository found at ${fs.canonicalize(".".toPath())}.")
				exitProcess(-1)
			}

			withDatabase(vc.configDirectory / "stacker.db") { db ->
				val stackManager = RealStackManager(db)
				val configManager = RealConfigManager(db, fs, stackManager)
				val locker = RealLocker(db, stackManager, vc)
				val httpClient = HttpClient(Curl) {
					install(ContentNegotiation) {
						json(
							Json {
								ignoreUnknownKeys = true
								decodeEnumsCaseInsensitive = true
							},
						)
					}
				}

				val remote = remoteOverride ?: GitHubRemote(httpClient, vc.originUrl, configManager)

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
