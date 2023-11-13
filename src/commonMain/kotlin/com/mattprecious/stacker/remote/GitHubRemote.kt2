package com.mattprecious.stacker.remote

import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.delegates.mutableLazy
import com.mattprecious.stacker.remote.Remote.PrInfo
import com.mattprecious.stacker.remote.Remote.PrResult
import org.kohsuke.github.GHFileNotFoundException
import org.kohsuke.github.GHIssueState
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

	private var gitHub: GitHub by mutableLazy { createGitHub(configManager.githubToken) }

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

	override fun openOrRetargetPullRequest(
		branchName: String,
		targetName: String,
		prInfo: () -> PrInfo,
	): PrResult {
		val pr = repo!!.queryPullRequests().head(branchName).list().firstOrNull()
		return if (pr == null) {
			val info = prInfo()

			// TODO: Drafts. Need to somehow know whether drafts are supported in the repo.
			val createdPr = repo!!.createPullRequest(info.title, branchName, targetName, info.body)

			PrResult.Created(url = createdPr.htmlUrl.toString())
		} else {
			pr.setBaseBranch(targetName)
			PrResult.Updated(pr.htmlUrl.toString())
		}
	}

	override fun getPrStatus(branchName: String): Remote.PrStatus {
		val pr = repo!!.queryPullRequests().head(branchName).state(GHIssueState.ALL).list().firstOrNull()
		return when {
			pr == null -> Remote.PrStatus.NotFound
			pr.isMerged -> Remote.PrStatus.Merged
			pr.state == GHIssueState.CLOSED -> Remote.PrStatus.Closed
			pr.state == GHIssueState.OPEN -> Remote.PrStatus.Open
			else -> throw IllegalStateException("Unable to determine status of PR #${pr.number} for branch $branchName.")
		}
	}

	private fun createGitHub(token: String?): GitHub {
		return GitHubBuilder()
			.withOAuthToken(token)
			.build()
	}
}
