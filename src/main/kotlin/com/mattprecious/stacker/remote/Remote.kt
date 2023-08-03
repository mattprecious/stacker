package com.mattprecious.stacker.remote

interface Remote {
	val isAuthenticated: Boolean
	val repoName: String?
	val hasRepoAccess: Boolean

	fun setToken(token: String): Boolean
}
