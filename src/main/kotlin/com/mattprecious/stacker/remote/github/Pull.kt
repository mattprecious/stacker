package com.mattprecious.stacker.remote.github

import kotlinx.serialization.Serializable

@Serializable
data class Pull(
	val number: Int,
	val merged_at: String?,
	val state: State,
	val html_url: String,
) {
	@Serializable
	enum class State {
		Open,
		Closed,
	}
}
