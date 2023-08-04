package com.mattprecious.stacker.config

import com.mattprecious.stacker.vc.BranchData
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

		repoConfig = RepoConfig(
			trunk = trunk,
			trailingTrunk = trailingTrunk,
		)

		if (currentConfig != null) {
			vc.setMetadata(currentConfig.trunk, null)
			if (currentConfig.trailingTrunk != null) {
				vc.setMetadata(currentConfig.trailingTrunk, null)
			}
		}

		vc.setMetadata(trunk, BranchData(isTrunk = true, parentName = null, children = listOfNotNull(trailingTrunk)))
		if (trailingTrunk != null) {
			vc.setMetadata(trailingTrunk, BranchData(isTrunk = true, parentName = trunk, children = emptyList()))
		}
	}
}
