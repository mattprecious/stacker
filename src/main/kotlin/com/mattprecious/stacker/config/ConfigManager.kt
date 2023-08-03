package com.mattprecious.stacker.config

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
