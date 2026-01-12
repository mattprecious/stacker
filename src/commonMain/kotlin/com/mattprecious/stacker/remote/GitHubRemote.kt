package com.mattprecious.stacker.remote

import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.remote.Remote.PrInfo
import com.mattprecious.stacker.remote.Remote.PrResult
import com.mattprecious.stacker.remote.github.CreatePull
import com.mattprecious.stacker.remote.github.GitHubError
import com.mattprecious.stacker.remote.github.Pull
import com.mattprecious.stacker.remote.github.UpdatePull
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMessageBuilder
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException

class GitHubRemote(
  private val client: HttpClient,
  private val originUrl: String,
  private val configManager: ConfigManager,
) : Remote {
  override val isAuthenticated: Boolean
    get() = configManager.githubToken?.let(::isTokenValid) == true

  private val repoOwnerAndName: Pair<String, String>? by lazy {
    val originMatchResult = Regex("""^.+github\.com:(.+)/(.+)\.git$""").matchEntire(originUrl)
    originMatchResult?.groupValues?.let { it[1] to it[2] }
  }

  override val repoName: String? by lazy { repoOwnerAndName?.let { "${it.first}/${it.second}" } }

  override val hasRepoAccess: Boolean
    get() = runBlocking { client.get("$host/repos/$repoName") { auth() }.status.isSuccess() }

  override fun setToken(token: String): Boolean {
    return isTokenValid(token).also { if (it) configManager.githubToken = token }
  }

  override fun openOrRetargetPullRequest(
    branchName: String,
    targetName: String,
    prInfo: () -> PrInfo,
  ): PrResult = runBlocking {
    val pr =
      client
        .get("$host/repos/$repoName/pulls") {
          auth()
          parameter("head", branchName.asHead())
        }
        .bodyOrThrow<List<Pull>>()
        .firstOrNull()

    return@runBlocking if (pr == null) {
      val createdPr =
        client
          .post("$host/repos/$repoName/pulls") {
            auth()

            val info = prInfo()
            contentType(ContentType.Application.Json)
            setBody(
              // TODO: Drafts. Need to somehow know whether drafts are supported in the repo.
              CreatePull(title = info.title, body = info.body, head = branchName, base = targetName)
            )
          }
          .bodyOrThrow<Pull>()

      PrResult.Created(url = createdPr.html_url, number = createdPr.number)
    } else if (pr.base.ref != targetName) {
      client
        .patch("$host/repos/$repoName/pulls/${pr.number}") {
          auth()
          contentType(ContentType.Application.Json)
          setBody(UpdatePull(base = targetName))
        }
        .requireSuccess()

      PrResult.Updated(url = pr.html_url, number = pr.number)
    } else {
      PrResult.NoChange(url = pr.html_url, number = pr.number)
    }
  }

  override fun getPrStatus(branchName: String): Remote.PrStatus = runBlocking {
    val pr =
      client
        .get("$host/repos/$repoName/pulls") {
          auth()
          parameter("head", branchName.asHead())
          parameter("state", "all")
        }
        .bodyOrThrow<List<Pull>>()
        .firstOrNull()

    return@runBlocking when {
      pr == null -> Remote.PrStatus.NotFound
      pr.merged_at != null -> Remote.PrStatus.Merged
      pr.state == Pull.State.Closed -> Remote.PrStatus.Closed
      pr.state == Pull.State.Open -> Remote.PrStatus.Open
      else ->
        throw IllegalStateException(
          "Unable to determine status of PR #${pr.number} for branch $branchName."
        )
    }
  }

  private fun String.asHead() = "${repoOwnerAndName!!.first}:$this"

  // TODO: Investigate using the Auth plugin further. It doesn't fit into the current API of this
  // class.
  private fun HttpMessageBuilder.auth() = bearerAuth(configManager.githubToken!!)

  private fun isTokenValid(token: String): Boolean = runBlocking {
    client.get("$host/rate_limit") { bearerAuth(token) }.status.value != 401
  }

  private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
    return if (status.isSuccess()) {
      body<T>()
    } else {
      throw asThrowable()
    }
  }

  private suspend fun HttpResponse.requireSuccess() {
    if (!status.isSuccess()) throw asThrowable()
  }

  private suspend fun HttpResponse.asThrowable(): Throwable {
    return IOException(
      """
			Received ${status.value} when calling ${call.request.url}.
			Message: ${body<GitHubError>().message}
			"""
        .trimIndent()
    )
  }
}

private const val host = "https://api.github.com"
