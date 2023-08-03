package com.mattprecious.stacker.config

import com.mattprecious.stacker.vc.BranchData
import com.mattprecious.stacker.vc.VersionControl
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.buffer
import okio.sink
import okio.source
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.exists

interface ConfigManager {
	val repoInitialized: Boolean
	val trunk: String?
	val trailingTrunk: String?

	fun initializeRepo(
		trunk: String,
		trailingTrunk: String?,
	)
}

class RealConfigManager(
	private val vc: VersionControl,
) : ConfigManager {
	private val repoConfigPath = vc.configDirectory / ".stacker_config"

	private val repoConfig: RepoConfig? by lazy {
		if (repoConfigPath.exists()) {
			val configJson = repoConfigPath.source().buffer().use { it.readUtf8() }
			Json.decodeFromString(configJson)
		} else {
			null
		}
	}

	override val repoInitialized: Boolean
		get() = repoConfig != null

	override val trunk: String?
		get() = repoConfig?.trunk

	override val trailingTrunk: String?
		get() = repoConfig?.trailingTrunk

	override fun initializeRepo(
		trunk: String,
		trailingTrunk: String?,
	) {
		saveRepoConfig(
			RepoConfig(
				trunk = trunk,
				trailingTrunk = trailingTrunk,
			),
		)
	}

	private fun saveRepoConfig(config: RepoConfig) {
		repoConfigPath.createParentDirectories()
		repoConfigPath.sink().buffer().use { it.writeUtf8(Json.encodeToString(config)) }

		val currentConfig = repoConfig
		if (currentConfig != null) {
			vc.setMetadata(currentConfig.trunk, null)
			if (currentConfig.trailingTrunk != null) {
				vc.setMetadata(currentConfig.trailingTrunk, null)
			}
		}

		vc.setMetadata(config.trunk, BranchData(isTrunk = true, parentName = null))
		if (config.trailingTrunk != null) {
			vc.setMetadata(config.trailingTrunk, BranchData(isTrunk = true, parentName = null))
		}
	}
}
