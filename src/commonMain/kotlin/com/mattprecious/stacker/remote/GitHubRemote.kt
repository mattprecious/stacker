package com.mattprecious.stacker.remote

import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.remote.Remote.PrResult
import com.mattprecious.stacker.remote.github.CreatePullRequest
import com.mattprecious.stacker.remote.github.Pull
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking

class GitHubRemote(
	private val client: HttpClient,
	private val originUrl: String,
	private val configManager: ConfigManager,
) : Remote {
	override val isAuthenticated: Boolean
		get() = configManager.githubToken?.let(::isTokenValid) == true

	override val repoName: String? by lazy {
		val originMatchResult = Regex("""^.+github\.com:(.+/.+)\.git$""").matchEntire(originUrl)
		originMatchResult?.groups?.get(1)?.value
	}

	override val hasRepoAccess: Boolean
		get() = runBlocking {
			client.get("https://api.github.com/repos/$repoName") {
				header("Authorization", "Bearer ${configManager.githubToken}")
			}.status.isSuccess()
		}

	override fun setToken(token: String): Boolean {
		return isTokenValid(token).also {
			if (it) {
				configManager.githubToken = token
			}
		}
	}

	override fun openOrRetargetPullRequest(
		branchName: String,
		targetName: String,
		prInfo: () -> Remote.PrInfo,
	): PrResult = runBlocking {
		val pr = client.get("https://api.github.com/repos/$repoName/pulls") {
			header("Authorization", "Bearer ${configManager.githubToken}")
			parameter("head", branchName)
		}.body<List<Pull>>().firstOrNull()

		return@runBlocking if (pr == null) {
			val info = prInfo()

			// TODO: Drafts. Need to somehow know whether drafts are supported in the repo.
			val createdPr = client.post("https://api.github.com/repos/$repoName/pulls") {
				header("Authorization", "Bearer ${configManager.githubToken}")
				contentType(ContentType.Application.Json)
				setBody(
					CreatePullRequest(
						title = info.title,
						body = info.body,
						head = branchName,
						base = targetName,
					),
				)
			}

			PrResult.Created(url = createdPr.body<Pull>().html_url)
		} else {
			client.patch("https://api.github.com/repos/$repoName/pulls/${pr.number}") {
				header("Authorization", "Bearer ${configManager.githubToken}")
				parameter("base", targetName)
			}
			PrResult.Updated(pr.html_url)
		}
	}

	override fun getPrStatus(branchName: String): Remote.PrStatus = runBlocking {
		val pr = client.get("https://api.github.com/repos/$repoName/pulls") {
			header("Authorization", "Bearer ${configManager.githubToken}")
			parameter("head", branchName)
			parameter("state", "all")
		}.body<List<Pull>>().firstOrNull()

		return@runBlocking when {
			pr == null -> Remote.PrStatus.NotFound
			pr.merged_at != null -> Remote.PrStatus.Merged
			pr.state == Pull.State.Closed -> Remote.PrStatus.Closed
			pr.state == Pull.State.Open -> Remote.PrStatus.Open
			else -> throw IllegalStateException("Unable to determine status of PR #${pr.number} for branch $branchName.")
		}
	}

	private fun isTokenValid(token: String): Boolean = runBlocking {
		client.get("https://api.github.com/rate_limit") {
			header("Authorization", "Bearer $token")
		}.status.value != 401
	}
}
