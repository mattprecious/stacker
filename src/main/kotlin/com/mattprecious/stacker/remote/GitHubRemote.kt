package com.mattprecious.stacker.remote

import com.mattprecious.stacker.config.ConfigManager
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

class GitHubRemote(
	private val configManager: ConfigManager,
) : Remote {
	private var gitHub = createGitHub(configManager.githubToken)

	override val isAuthenticated: Boolean
		get() = gitHub.isCredentialValid

	override fun setToken(token: String): Boolean {
		val newGitHub = createGitHub(token)
		return if (newGitHub.isCredentialValid) {
			configManager.githubToken = token
			gitHub = newGitHub
			true
		} else {
			false
		}
	}

	private fun createGitHub(token: String?): GitHub {
		return GitHubBuilder()
			.withOAuthToken(token)
			.build()
	}
}
