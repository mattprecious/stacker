package com.mattprecious.stacker.remote

import com.mattprecious.stacker.config.ConfigManager
import org.kohsuke.github.GHFileNotFoundException
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

class GitHubRemote(
	private val originUrl: String,
	private val configManager: ConfigManager,
) : Remote {
	override val isAuthenticated: Boolean
		get() = gitHub.isCredentialValid

	override val repoName: String? by lazy {
		val originMatchResult = Regex("""^.+github\.com:(.+/.+)\.git$""").matchEntire(originUrl)
		originMatchResult?.groups?.get(1)?.value
	}

	override val hasRepoAccess: Boolean
		get() = repo != null

	private val repo: GHRepository? by lazy {
		try {
			gitHub.getRepository(repoName)
		} catch (t: GHFileNotFoundException) {
			// We don't have access.
			null
		}
	}

	private var gitHub = createGitHub(configManager.githubToken)

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
