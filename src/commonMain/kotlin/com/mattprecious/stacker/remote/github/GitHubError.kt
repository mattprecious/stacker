
package com.mattprecious.stacker.remote.github

import kotlinx.serialization.Serializable

@Serializable
data class GitHubError(
	val message: String,
)
