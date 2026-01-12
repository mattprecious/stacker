package com.mattprecious.stacker.vc

import okio.Path

interface VersionControl : AutoCloseable {
  val repoDiscovered: Boolean
  val configDirectory: Path
  val currentBranchName: String
  val originUrl: String?
  val branches: List<String>

  /** The default branch name from git config. Not necessarily the trunk branch in this repo. */
  val defaultBranch: String?

  fun checkBranches(branchNames: Set<String>): Set<String>

  fun checkout(branchName: String)

  enum class BranchCreateResult {
    Success,
    AlreadyExists,
  }

  fun createBranchFromCurrent(branchName: String): BranchCreateResult

  fun renameBranch(branchName: String, newName: String)

  fun delete(branchName: String)

  fun pushBranches(branchNames: List<String>)

  fun pull(branchName: String)

  fun latestCommitInfo(branchName: String): CommitInfo

  fun isAncestor(branchName: String, possibleAncestorName: String): Boolean

  fun restack(branchName: String, parentName: String, parentSha: String): Boolean

  fun getSha(branch: String): String

  fun abortRebase()

  fun continueRebase(branchName: String): Boolean

  data class CommitInfo(val title: String, val body: String?)
}
