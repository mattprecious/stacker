package com.mattprecious.stacker.config

import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.db.RepoConfig
import com.mattprecious.stacker.db.RepoDatabase
import com.mattprecious.stacker.delegates.Permissions
import com.mattprecious.stacker.delegates.jsonFile
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.stack.StackManager
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.posix.getpwuid
import platform.posix.getuid

class RealConfigManager(
	db: RepoDatabase,
	fs: FileSystem,
	private val stackManager: StackManager,
) : ConfigManager {
	private val repoConfigQueries = db.repoConfigQueries

	private var userConfig: UserConfig by jsonFile(
		fs = fs,
		path = getpwuid(getuid())!!.pointed.pw_dir!!.toKString().toPath() / ".stacker_user_config",
		createPermissions = Permissions.Posix(setOf(Permissions.Posix.Permission.OwnerRead)),
		maximumAllowedPermissions = Permissions.Posix(
			setOf(
				Permissions.Posix.Permission.OwnerRead,
				Permissions.Posix.Permission.OwnerWrite,
			),
		),
	) {
		UserConfig(
			githubToken = null,
		)
	}

	override val repoInitialized: Boolean
		get() = repoConfigQueries.initialized().executeAsOne()

	override val repoConfig: RepoConfig
		get() = repoConfigQueries.select().executeAsOne()

	override val trunk: String
		get() = repoConfigQueries.trunk().executeAsOne()

	override val trailingTrunk: String?
		get() = repoConfigQueries.trailingTrunk().executeAsOne().trailingTrunk

	override var githubToken: String?
		get() = userConfig.githubToken
		set(value) {
			userConfig = userConfig.copy(githubToken = value)
		}

	override suspend fun initializeRepo(
		scope: StackerCommandScope,
		trunk: String,
		trunkSha: String,
		trailingTrunk: String?,
	) {
		val currentConfig = if (repoInitialized) repoConfig else null

		val currentTrunkBranch = currentConfig?.trunk?.let(stackManager::getBranch)
		val currentTrailingTrunkBranch = currentConfig?.trailingTrunk?.let(stackManager::getBranch)

		val trunkChanging = trunk != currentConfig?.trunk
		val trailingTrunkChanging = trailingTrunk != currentConfig?.trailingTrunk

		if (!trunkChanging && !trailingTrunkChanging) return

		val trailingChangingWithChildren = trailingTrunkChanging && currentTrailingTrunkBranch?.children?.isNotEmpty() == true
		val trunkChangingWithChildren = trunkChanging && currentTrunkBranch?.children?.any { it != currentTrailingTrunkBranch } == true
		if (trunkChangingWithChildren) {
			scope.printStaticError(
				buildAnnotatedString {
					append("Cannot change trunk. Current trunk branch ")
					branch { append(currentTrunkBranch.name) }
					append(" has children.")
				},
			)

			scope.abort()
		}

		if (trailingChangingWithChildren) {
			scope.printStaticError(
				buildAnnotatedString {
					append("Cannot change trailing trunk. Current trailing trunk branch ")
					branch { append(currentTrailingTrunkBranch.name) }
					append(" has children.")
				},
			)

			scope.abort()
		}

		if (trailingTrunkChanging) {
			currentTrailingTrunkBranch?.value?.let(stackManager::untrackBranch)
		}

		if (trunkChanging) {
			currentTrunkBranch?.value?.let(stackManager::untrackBranch)
		}

		repoConfigQueries.insert(
			trunk = trunk,
			trailingTrunk = trailingTrunk,
		)

		if (trunkChanging) {
			stackManager.trackBranch(branchName = trunk, parentName = null, parentSha = null)
		}

		if (trailingTrunkChanging) {
			trailingTrunk?.let { stackManager.trackBranch(branchName = it, parentName = trunk, parentSha = trunkSha) }
		}
	}
}
