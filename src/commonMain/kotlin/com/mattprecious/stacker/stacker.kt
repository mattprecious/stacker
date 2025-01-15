package com.mattprecious.stacker

import com.github.ajalt.clikt.core.main
import com.mattprecious.stacker.cli.StackerCli
import com.mattprecious.stacker.config.RealConfigManager
import com.mattprecious.stacker.lock.RealLocker
import com.mattprecious.stacker.remote.GitHubRemote
import com.mattprecious.stacker.remote.NoRemote
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.shell.RealShell
import com.mattprecious.stacker.stack.RealStackManager
import com.mattprecious.stacker.vc.GitVersionControl
import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.posix.getenv
import kotlin.system.exitProcess

fun main(args: Array<String>) {
	try {
		val terminal = memScoped { getenv("TERM")?.toKString() ?: "" }

		runBlocking {
			withStacker(useFancySymbols = supportsFancySymbols(terminal)) {
				StackerCli(it).main(args)
			}
		}
	} catch (e: RepoNotFoundException) {
		println(e.message)
		exitProcess(-1)
	}
}

private fun supportsFancySymbols(term: String): Boolean {
	return term == "xterm-ghostty" || term == "xterm-kitty"
}

@OptIn(ExperimentalSerializationApi::class)
internal suspend fun withStacker(
	fileSystem: FileSystem = FileSystem.SYSTEM,
	remoteOverride: Remote? = null,
	useFancySymbols: Boolean = false,
	block: suspend (StackerDeps) -> Unit,
) {
	memScoped {
		val shell = RealShell()
		GitVersionControl(this, fileSystem, shell).use { vc ->
			if (!vc.repoDiscovered) {
				throw RepoNotFoundException("No repository found at ${fileSystem.canonicalize(".".toPath())}.")
			}

			withDatabase(vc.configDirectory / "stacker.db") { db ->
				val stackManager = RealStackManager(db)

				// TODO: Do this somewhere else...
				stackManager.untrackBranches(vc.checkBranches(stackManager.trackedBranchNames.toSet()))

				val configManager = RealConfigManager(db, fileSystem, stackManager)
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

				val originUrl = vc.originUrl
				val remote = when {
					remoteOverride != null -> remoteOverride
					originUrl != null -> GitHubRemote(httpClient, originUrl, configManager)
					else -> NoRemote()
				}

				block(
					StackerDeps(
						configManager = configManager,
						locker = locker,
						remote = remote,
						stackManager = stackManager,
						useFancySymbols = useFancySymbols,
						vc = vc,
					),
				)
			}
		}
	}
}

class RepoNotFoundException(message: String) : RuntimeException(message)
