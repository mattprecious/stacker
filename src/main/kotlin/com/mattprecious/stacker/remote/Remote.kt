package com.mattprecious.stacker.remote

interface Remote {
	val isAuthenticated: Boolean

	fun setToken(token: String): Boolean
}
