package com.mattprecious.stacker.config

import com.mattprecious.stacker.vc.VersionControl
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.util.*
import kotlin.io.path.div

class RealConfigManager(
	private val vc: VersionControl,
) : ConfigManager {
	private var userConfig: UserConfig by jsonFile(
		path = Path.of(System.getProperty("user.home")) / ".stacker_user_config",
		permissions = EnumSet.of(
			PosixFilePermission.OWNER_READ,
			PosixFilePermission.OWNER_WRITE,
		),
	) {
		UserConfig(
			githubToken = null,
		)
	}

	private var repoConfig: RepoConfig? by jsonFile(path = vc.configDirectory / ".stacker_config") { null }

	override val repoInitialized: Boolean
		get() = repoConfig != null

	override val trunk: String?
		get() = repoConfig?.trunk

	override val trailingTrunk: String?
		get() = repoConfig?.trailingTrunk

	override var githubToken: String?
		get() = userConfig.githubToken
		set(value) {
			userConfig = userConfig.copy(githubToken = value)
		}

	override fun initializeRepo(
		trunk: String,
		trailingTrunk: String?,
	) {
		val currentConfig = repoConfig

		// TODO: All this VC stuff probably shouldn't be here.
		val currentTrunkBranch = currentConfig?.trunk?.let(vc::getBranch)
		val currentTrailingTrunkBranch = currentConfig?.trailingTrunk?.let(vc::getBranch)

		val trunkChanging = trunk != currentConfig?.trunk
		val trailingTrunkChanging = trailingTrunk != currentConfig?.trailingTrunk

		if (!trunkChanging && !trailingTrunkChanging) return

		val trailingChangingWithChildren = trailingTrunkChanging && currentTrailingTrunkBranch?.children?.isNotEmpty() == true
		val trunkChangingWithChildren = trunkChanging && currentTrunkBranch?.children?.any { it != currentTrailingTrunkBranch } != false
		require(!trunkChangingWithChildren && !trailingChangingWithChildren)

		if (trailingTrunkChanging) {
			currentTrailingTrunkBranch?.untrack()
		}

		if (trunkChanging) {
			currentTrunkBranch?.untrack()
		}

		repoConfig = RepoConfig(
			trunk = trunk,
			trailingTrunk = trailingTrunk,
		)

		val trunkBranch = if (trunkChanging) vc.getBranch(trunk) else currentTrunkBranch!!

		if (trunkChanging) {
			trunkBranch.track(isTrunk = true, parent = null)
		}

		if (trailingTrunkChanging) {
			trailingTrunk?.let(vc::getBranch)?.track(isTrunk = true, parent = trunkBranch)
		}
	}
}
