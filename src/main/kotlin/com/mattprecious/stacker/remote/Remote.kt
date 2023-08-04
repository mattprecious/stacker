package com.mattprecious.stacker.remote

interface Remote {
	val isAuthenticated: Boolean
	val repoName: String?
	val hasRepoAccess: Boolean

	fun setToken(token: String): Boolean

	fun openOrRetargetPullRequest(
		branchName: String,
		targetName: String,
		prInfo: () -> PrInfo,
	): PrResult

	data class PrInfo(
		val title: String,
		val body: String?,
	)

	sealed interface PrResult {
		val url: String

		data class Created(override val url: String) : PrResult
		data class Updated(override val url: String) : PrResult
	}
}
