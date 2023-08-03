package com.mattprecious.stacker.config

import com.mattprecious.stacker.vc.BranchData
import com.mattprecious.stacker.vc.VersionControl
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.buffer
import okio.sink
import okio.source
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.util.*
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.notExists

interface ConfigManager {
	val repoInitialized: Boolean
	val trunk: String?
	val trailingTrunk: String?

	var githubToken: String?

	fun initializeRepo(
		trunk: String,
		trailingTrunk: String?,
	)
}

class RealConfigManager(
	private val vc: VersionControl,
) : ConfigManager {
	private val userConfigDirectory = Path.of(System.getProperty("user.home"))
	private val userConfigPath = userConfigDirectory / ".stacker_user_config"

	private val repoConfigPath = vc.configDirectory / ".stacker_config"

	private val userConfig: UserConfig by lazy {
		if (userConfigPath.exists()) {
			val configJson = userConfigPath.source().buffer().use { it.readUtf8() }
			Json.decodeFromString(configJson)
		} else {
			UserConfig(
				githubToken = null,
			)
		}
	}

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

	override var githubToken: String?
		get() = userConfig.githubToken
		set(value) {
			saveUserConfig(userConfig.copy(githubToken = value))
		}

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

	private fun saveUserConfig(config: UserConfig) {
		userConfigPath.createParentDirectories()
		if (userConfigPath.notExists()) {
			val permissions = EnumSet.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
			)
			userConfigPath.createFile(PosixFilePermissions.asFileAttribute(permissions))
		}

		userConfigPath.sink().buffer().use { it.writeUtf8(Json.encodeToString(config)) }
	}
}
