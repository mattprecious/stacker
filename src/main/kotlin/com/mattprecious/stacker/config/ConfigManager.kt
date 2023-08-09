package com.mattprecious.stacker.config

import com.mattprecious.stacker.db.RepoConfig

interface ConfigManager {
	val repoInitialized: Boolean
	val repoConfig: RepoConfig
	val trunk: String?
	val trailingTrunk: String?

	var githubToken: String?

	fun initializeRepo(
		trunk: String,
		trailingTrunk: String?,
	)
}
