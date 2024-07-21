package com.mattprecious.stacker.remote

interface Remote {
	val isAuthenticated: Boolean
	val repoName: String?
	val hasRepoAccess: Boolean

	fun setToken(token: String): Boolean

	fun getPrStatus(branchName: String): PrStatus

	fun openOrRetargetPullRequest(
		branchName: String,
		targetName: String,
		prInfo: () -> PrInfo,
	): PrResult

	data class PrInfo(
		val title: String,
		val body: String?,
	)

	enum class PrStatus {
		NotFound,
		Open,
		Closed,
		Merged,
	}

	sealed interface PrResult {
		val url: String
		val number: Long

		data class Created(
			override val url: String,
			override val number: Long,
		) : PrResult

		data class Updated(
			override val url: String,
			override val number: Long,
		) : PrResult

		data class NoChange(
			override val url: String,
			override val number: Long,
		) : PrResult
	}
}
