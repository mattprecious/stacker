package com.mattprecious.stacker.vc

import com.github.git2_h
import com.github.git2_h.C_POINTER
import com.github.git2_h.GIT_BRANCH_LOCAL
import com.github.git2_h.GIT_CHECKOUT_OPTIONS_VERSION
import com.github.git2_h.GIT_CREDTYPE_SSH_KEY
import com.github.git2_h.GIT_EAPPLIED
import com.github.git2_h.GIT_ECONFLICT
import com.github.git2_h.GIT_ENOTFOUND
import com.github.git2_h.GIT_EUNMERGED
import com.github.git2_h.GIT_FETCH_OPTIONS_VERSION
import com.github.git2_h.GIT_ITEROVER
import com.github.git2_h.GIT_MERGE_ANALYSIS_FASTFORWARD
import com.github.git2_h.GIT_MERGE_ANALYSIS_UP_TO_DATE
import com.github.git2_h.GIT_MERGE_PREFERENCE_NO_FASTFORWARD
import com.github.git2_h.GIT_OK
import com.github.git2_h.GIT_PUSH_OPTIONS_VERSION
import com.github.git2_h.GIT_REBASE_NO_OPERATION
import com.github.git2_h.GIT_REBASE_OPERATION_PICK
import com.github.git2_h.GIT_REMOTE_CALLBACKS_VERSION
import com.github.git2_h.git_annotated_commit_from_revspec
import com.github.git2_h.git_annotated_commit_id
import com.github.git2_h.git_annotated_commit_lookup
import com.github.git2_h.git_branch_create
import com.github.git2_h.git_branch_delete
import com.github.git2_h.git_branch_iterator_free
import com.github.git2_h.git_branch_iterator_new
import com.github.git2_h.git_branch_move
import com.github.git2_h.git_branch_next
import com.github.git2_h.git_checkout_options_init
import com.github.git2_h.git_checkout_tree
import com.github.git2_h.git_commit_body
import com.github.git2_h.git_commit_lookup
import com.github.git2_h.git_commit_summary
import com.github.git2_h.git_config_get_string
import com.github.git2_h.git_config_snapshot
import com.github.git2_h.git_credential_ssh_key_from_agent
import com.github.git2_h.git_error_last
import com.github.git2_h.git_fetch_options_init
import com.github.git2_h.git_libgit2_init
import com.github.git2_h.git_libgit2_shutdown
import com.github.git2_h.git_merge_analysis_for_ref
import com.github.git2_h.git_merge_base
import com.github.git2_h.git_oid_equal
import com.github.git2_h.git_oid_fromstr
import com.github.git2_h.git_oid_tostr_s
import com.github.git2_h.git_push_options_init
import com.github.git2_h.git_rebase_abort
import com.github.git2_h.git_rebase_commit
import com.github.git2_h.git_rebase_finish
import com.github.git2_h.git_rebase_free
import com.github.git2_h.git_rebase_init
import com.github.git2_h.git_rebase_next
import com.github.git2_h.git_rebase_open
import com.github.git2_h.git_rebase_operation_byindex
import com.github.git2_h.git_rebase_operation_current
import com.github.git2_h.git_reference_lookup
import com.github.git2_h.git_reference_name_to_id
import com.github.git2_h.git_reference_set_target
import com.github.git2_h.git_reference_shorthand
import com.github.git2_h.git_remote_fetch
import com.github.git2_h.git_remote_free
import com.github.git2_h.git_remote_init_callbacks
import com.github.git2_h.git_remote_lookup
import com.github.git2_h.git_remote_push
import com.github.git2_h.git_remote_url
import com.github.git2_h.git_repository_config
import com.github.git2_h.git_repository_discover
import com.github.git2_h.git_repository_free
import com.github.git2_h.git_repository_head
import com.github.git2_h.git_repository_open
import com.github.git2_h.git_repository_path
import com.github.git2_h.git_repository_set_head
import com.github.git2_h.git_revparse_single
import com.github.git2_h.git_signature_default
import com.github.git2_h.git_signature_free
import com.github.git_buf
import com.github.git_checkout_options
import com.github.git_credential_acquire_cb
import com.github.git_error
import com.github.git_fetch_options
import com.github.git_oid
import com.github.git_push_options
import com.github.git_rebase_operation
import com.github.git_remote_callbacks
import com.github.git_strarray
import com.github.git_transport_certificate_check_cb
import com.mattprecious.stacker.shell.Shell
import com.mattprecious.stacker.vc.VersionControl.CommitInfo
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySegment.NULL
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.nio.file.Path

class GitVersionControl(
	arena: Arena,
	private val shell: Shell,
) : VersionControl {
	private val nullableRepo: MemorySegment?
	private val repo: MemorySegment
		get() = nullableRepo!!

	override val repoDiscovered: Boolean
		get() = nullableRepo != null
	override val configDirectory: Path
		get() = Path.of(git_repository_path(repo).utf8())

	override val currentBranchName: String
		get() = arena { git_reference_shorthand(getHead()).utf8() }

	override val originUrl: String
		get() = arena { git_remote_url(getOrigin()).utf8() }

	override val branches: List<String>
		get() = arena {
			val flags = allocateInt(GIT_BRANCH_LOCAL())
			val iterator = withAllocate { checkError(git_branch_iterator_new(it, repo, GIT_BRANCH_LOCAL())) }.deref()
			val list = mapBranches(iterator, flags) { git_reference_shorthand(it).utf8() }
			git_branch_iterator_free(iterator)
			return@arena list
		}

	override val editor: String?
		get() = arena {
			val config = withAllocate { checkError(git_repository_config(it, repo)) }.deref()
			val configSnapshot = withAllocate { checkError(git_config_snapshot(it, config)) }.deref()

			return@arena try {
				val editor = withAllocate {
					val code = git_config_get_string(it, configSnapshot, allocate("core.editor"))
					if (code == ReturnCodes.ENOTFOUND) return@arena null
					checkError(code)
				}

				editor.deref().utf8()
			} finally {
				git2_h.git_config_free(configSnapshot)
				git2_h.git_config_free(config)
			}
		}

	init {
		loadLibGit2()
		git_libgit2_init()

		val repoPathBuf = with(arena) { repoPath() }

		nullableRepo = if (repoPathBuf == null) {
			null
		} else {
			val repo = arena.withAllocate {
				val repoPath = git_buf.`ptr$get`(repoPathBuf)
				checkError(git_repository_open(it, repoPath))
			}.deref()
			git2_h.git_buf_dispose(repoPathBuf)
			repo
		}
	}

	/** @return git_buf */
	private fun Arena.repoPath(): MemorySegment? {
		val currentPath = allocate(System.getProperty("user.dir"))
		return withAllocate(git_buf.`$LAYOUT`()) {
			val code = git_repository_discover(it, currentPath, 0, NULL)
			if (code == ReturnCodes.ENOTFOUND) return null
			checkError(code)
		}
	}

	override fun close() {
		// Not sure if this is needed since the process is being killed.
		git_repository_free(repo)
		git_libgit2_shutdown()
	}

	override fun fallthrough(commands: List<String>) {
		shell.exec("git", *commands.toTypedArray())
	}

	override fun checkBranches(branchNames: Set<String>): Set<String> {
		return branchNames.toMutableSet().apply { removeAll(branches.toSet()) }
	}

	override fun checkout(branchName: String) = arena {
		val treeish = withAllocate { checkError(git_revparse_single(it, repo, this.allocate(branchName))) }.deref()
		checkout(branchName, treeish)
	}

	override fun createBranchFromCurrent(branchName: String) = arena {
		val commit = getCommitForBranch(currentBranchName)
		withAllocate { checkError(git_branch_create(it, repo, allocate(branchName), commit, 0)) }
		checkout(branchName, commit)
	}

	override fun renameBranch(branchName: String, newName: String): Unit = arena {
		withAllocate { checkError(git_branch_move(it, getBranch(branchName), allocate(newName), 0)) }
	}

	override fun delete(branchName: String): Unit = arena {
		checkError(git_branch_delete(getBranch(branchName)))
	}

	override fun latestCommitInfo(branchName: String): CommitInfo = arena {
		val commit = getCommitForBranch(branchName)
		val title = git_commit_summary(commit).utf8()
		val body = git_commit_body(commit).utf8OrNull()

		return@arena CommitInfo(
			title = title,
			body = body,
		)
	}

	override fun isAncestor(branchName: String, possibleAncestorName: String): Boolean = arena {
		val branchCommitId = getCommitId(branchName)
		val possibleAncestorCommitId = getCommitId(possibleAncestorName)

		val base = withAllocate(git_oid.`$LAYOUT`()) {
			val code = git_merge_base(it, repo, possibleAncestorCommitId, branchCommitId)
			if (code == ReturnCodes.ENOTFOUND) return@arena false
			checkError(code)
		}

		return@arena git_oid_equal(base, possibleAncestorCommitId) == 1
	}

	override fun restack(branchName: String, parentName: String, parentSha: String): Boolean = arena {
		val branchCommit = getAnnotatedCommit(branchName)
		val ontoCommit = getAnnotatedCommit(parentName)

		val upstreamId =
			withAllocate(git_oid.`$LAYOUT`()) { checkError(git_oid_fromstr(it, allocate(parentSha))) }
		val upstreamCommit = withAllocate { checkError(git_annotated_commit_lookup(it, repo, upstreamId)) }.deref()

		val rebase = withAllocate {
			checkError(git_rebase_init(it, repo, branchCommit, upstreamCommit, ontoCommit, NULL))
		}.deref()

		val result = performRebase(rebase, branchName)
		git_rebase_free(rebase)
		return@arena result
	}

	override fun getSha(branch: String): String = arena {
		return@arena git_oid_tostr_s(getCommitId(branch)).utf8()
	}

	override fun abortRebase() = arena {
		val rebase = getRebase() ?: return@arena
		checkError(git_rebase_abort(rebase))
		git_rebase_free(rebase)
	}

	override fun continueRebase(branchName: String): Boolean = arena {
		val rebase = getRebase() ?: return@arena true // Not really...
		val result = performRebase(rebase, branchName)
		git_rebase_free(rebase)
		return@arena result
	}

	override fun pushBranches(branchNames: List<String>): Unit = arena {
		// Libgit2 auth doesn't work on enterprise repos for some reason. Fall back to shell command for now.
		shell.exec("git", "push", "-f", "--atomic", "origin", *branchNames.toTypedArray())
	}

	private fun pushBranchesLibGit(branchNames: List<String>): Unit = arena {
		val strings = allocate(branchNames.map { it.asBranchRevSpec() }.map { "+$it:$it" })

		val refs = allocate(git_strarray.`$LAYOUT`())
		git_strarray.`count$set`(refs, branchNames.size.toLong())
		git_strarray.`strings$set`(refs, strings)

		// TODO: Atomic? I don't think libgit2 supports this.
		val options = withAllocate(git_push_options.`$LAYOUT`()) {
			checkError(git_push_options_init(it, GIT_PUSH_OPTIONS_VERSION()))
		}

		git_push_options.`callbacks$slice`(options).copyFrom(createRemoteCallbacks())

		val origin = getOrigin()
		checkError(git_remote_push(origin, refs, options))
		git_remote_free(origin)
	}

	override fun pull(branchName: String) {
		// Libgit2 auth doesn't work on enterprise repos for some reason. Fall back to shell command for now.
		val currentBranch = currentBranchName
		checkout(branchName)
		shell.exec("git", "pull", "origin", branchName)
		checkout(currentBranch)
	}

	private fun pullLibGit(branchName: String): Unit = arena {
		val pullOptions = withAllocate(git_fetch_options.`$LAYOUT`()) {
			checkError(git_fetch_options_init(it, GIT_FETCH_OPTIONS_VERSION()))
		}

		git_fetch_options.`callbacks$slice`(pullOptions).copyFrom(createRemoteCallbacks())

		val origin = getOrigin()
		checkError(git_remote_fetch(origin, allocate(listOf(branchName)), pullOptions, NULL))
		git_remote_free(origin)

		val head = withAllocate {
			checkError(git_annotated_commit_from_revspec(it, repo, allocate(branchName.asRemoteBranchRevSpec())))
		}.deref()

		val analysis = getMergeAnalysis(getBranch(branchName), head)
		if (analysis.upToDate) {
			return@arena
		}

		check(analysis.fastForward && !analysis.preferenceNoFastForward) {
			"$branchName cannot be fast-forwarded."
		}

		val commitId = git_annotated_commit_id(head)
		if (currentBranchName == branchName) {
			checkoutTree(commitId)
		}

		setBranchTarget(branchName, commitId)
	}

	/** @return git_remote_callbacks */
	private fun Arena.createRemoteCallbacks(): MemorySegment {
		val acquireCredentialCb = git_credential_acquire_cb { out, _, username, types, _ ->
			check(types and GIT_CREDTYPE_SSH_KEY() == GIT_CREDTYPE_SSH_KEY()) {
				"Unsupported credential types: $types"
			}

			checkError(git_credential_ssh_key_from_agent(out, username))

			return@git_credential_acquire_cb 0
		}

		val acquireCredential = git_credential_acquire_cb.allocate(acquireCredentialCb, this)

		// Is this bad? I don't know why it's not a known host.
		val certificateCheckCb = git_transport_certificate_check_cb { _, valid, _, _ -> valid }
		val certificateCheck = git_transport_certificate_check_cb.allocate(certificateCheckCb, this)

		val callbacks = withAllocate(git_remote_callbacks.`$LAYOUT`()) {
			checkError(git_remote_init_callbacks(it, GIT_REMOTE_CALLBACKS_VERSION()))
		}
		git_remote_callbacks.`credentials$set`(callbacks, acquireCredential)
		git_remote_callbacks.`certificate_check$set`(callbacks, certificateCheck)

		return callbacks
	}

	private fun <T> arena(block: Arena.() -> T) = Arena.ofConfined().use(block)

	private fun checkError(result: Int) {
		check(result == ReturnCodes.OK) {
			val message = git_error.`message$get`(git_error_last()).utf8()
			"Exit code: $result\n$message"
		}
	}

	/** Convert a branch name to a refspec. */
	private fun String.asBranchRevSpec() = "refs/heads/$this"

	/** Convert a branch name to a remote refspec. */
	private fun String.asRemoteBranchRevSpec() = "refs/remotes/origin/$this"

	/** @return git_oid* */
	private fun Arena.getCommitId(branchName: String): MemorySegment {
		return withAllocate(git_oid.`$LAYOUT`()) {
			checkError(git_reference_name_to_id(it, repo, allocate(branchName.asBranchRevSpec())))
		}
	}

	/** @return git_commit* */
	private fun Arena.getCommitForBranch(branchName: String): MemorySegment {
		return withAllocate { checkError(git_commit_lookup(it, repo, getCommitId(branchName))) }.deref()
	}

	/** @return git_reference* */
	private fun Arena.getHead(): MemorySegment {
		return withAllocate { checkError(git_repository_head(it, repo)) }.deref()
	}

	/** @return git_reference* */
	private fun Arena.getBranch(branchName: String): MemorySegment {
		return withAllocate { checkError(git_reference_lookup(it, repo, allocate(branchName.asBranchRevSpec()))) }.deref()
	}

	/** @return git_remote*, should be freed with [git_remote_free]. */
	private fun Arena.getOrigin(): MemorySegment {
		return withAllocate { checkError(git_remote_lookup(it, repo, allocate("origin"))) }.deref()
	}

	/** @param treeish git_commit* or git_object* */
	private fun Arena.checkout(branchName: String, treeish: MemorySegment) {
		checkoutTree(treeish)
		checkError(git_repository_set_head(repo, allocate(branchName.asBranchRevSpec())))
	}

	/** @param treeish git_commit* or git_object* */
	private fun Arena.checkoutTree(treeish: MemorySegment) {
		val options = withAllocate(git_checkout_options.`$LAYOUT`()) {
			git_checkout_options_init(it, GIT_CHECKOUT_OPTIONS_VERSION())
		}

		checkError(git_checkout_tree(repo, treeish, options))
	}

	private data class MergeAnalysis(
		private val analysisFlags: Int,
		private val mergePreferenceFlags: Int,
	) {
		val upToDate = analysisFlags and GIT_MERGE_ANALYSIS_UP_TO_DATE() != 0
		val fastForward = analysisFlags and GIT_MERGE_ANALYSIS_FASTFORWARD() != 0
		val preferenceNoFastForward = mergePreferenceFlags and GIT_MERGE_PREFERENCE_NO_FASTFORWARD() != 0
	}

	/**
	 * @param into git_reference*
	 * @param commit git_annotated_commit*
	 */
	private fun getMergeAnalysis(
		into: MemorySegment,
		commit: MemorySegment,
	): MergeAnalysis = arena {
		val analysis = allocate(C_POINTER)
		val mergePreference = allocate(C_POINTER)
		val heads = allocate(JAVA_LONG, commit.address())

		checkError(git_merge_analysis_for_ref(analysis, mergePreference, repo, into, heads, 1))

		return@arena MergeAnalysis(
			analysisFlags = analysis.get(JAVA_INT, 0),
			mergePreferenceFlags = mergePreference.get(JAVA_INT, 0),
		)
	}

	/**
	 * @param iterator git_branch_iterator*
	 * @param flags int, one of GIT_BRANCH_*.
	 * @param transform git_reference*
	 */
	private fun <T> Arena.mapBranches(
		iterator: MemorySegment,
		flags: MemorySegment,
		transform: (branch: MemorySegment) -> T,
	): List<T> {
		return buildList {
			forEachBranch(iterator, flags) { add(transform(it)) }
		}
	}

	/**
	 * @param iterator git_branch_iterator*
	 * @param flags int, one of GIT_BRANCH_*.
	 * @param action git_reference*
	 */
	private fun Arena.forEachBranch(
		iterator: MemorySegment,
		flags: MemorySegment,
		action: (branch: MemorySegment) -> Unit,
	) {
		while (true) {
			val branch = withAllocate {
				val code = git_branch_next(it, flags, iterator)
				if (code == ReturnCodes.ITEROVER) return
			}.deref()

			action(branch)
		}
	}

	/** @return git_annotated_commit* */
	private fun Arena.getAnnotatedCommit(branchName: String): MemorySegment {
		return withAllocate {
			checkError(git_annotated_commit_from_revspec(it, repo, allocate(branchName.asBranchRevSpec())))
		}.deref()
	}

	/** @return git_rebase* */
	private fun Arena.getRebase(): MemorySegment? {
		return withAllocate {
			val code = git_rebase_open(it, repo, NULL)
			if (code == ReturnCodes.ENOTFOUND) return null
			checkError(code)
		}.deref()
	}

	/** @param rebase git_rebase* */
	private fun Arena.performRebase(
		rebase: MemorySegment,
		branchName: String,
	): Boolean {
		val signature = withAllocate { checkError(git_signature_default(it, repo)) }.deref()

		forEachRebaseOperation(rebase) {
			if (!performRebaseOperation(rebase = rebase, operation = it, committer = signature)) {
				return@performRebase false
			}
		}

		checkError(git_rebase_finish(rebase, signature))
		git_signature_free(signature)

		val headId = withAllocate(git_oid.`$LAYOUT`()) {
			checkError(git_reference_name_to_id(it, repo, allocate("HEAD")))
		}

		setBranchTarget(branchName, headId)
		return true
	}

	/** @param target git_oid* */
	private fun Arena.setBranchTarget(
		branchName: String,
		target: MemorySegment,
	) {
		checkError(git_reference_set_target(allocate(C_POINTER), getBranch(branchName), target, NULL))
	}

	/**
	 * @param rebase git_rebase*
	 * @param action git_rebase_operation*
	 */
	private inline fun Arena.forEachRebaseOperation(
		rebase: MemorySegment,
		action: (operation: MemorySegment) -> Unit,
	) {
		val operation = allocate(C_POINTER)

		val operationIndex = git_rebase_operation_current(rebase)
		if (operationIndex != GIT_REBASE_NO_OPERATION()) {
			action(git_rebase_operation_byindex(rebase, operationIndex))
		}

		while (true) {
			val code = git_rebase_next(operation, rebase)
			if (code == ReturnCodes.ITEROVER) break
			checkError(code)
			action(operation.deref())
		}
	}

	/**
	 * @param rebase git_rebase*
	 * @param operation git_rebase_operation*
	 * @param committer git_signature*
	 */
	private fun Arena.performRebaseOperation(
		rebase: MemorySegment,
		operation: MemorySegment,
		committer: MemorySegment,
	): Boolean {
		val operationType = git_rebase_operation.`type$get`(operation)
		check(operationType == GIT_REBASE_OPERATION_PICK())
		val code = git_rebase_commit(allocate(git_oid.`$LAYOUT`()), rebase, NULL, committer, NULL, NULL)
		if (code == ReturnCodes.EAPPLIED) return true
		// TODO: Should these return different results?
		if (code == ReturnCodes.EUNMERGED || code == ReturnCodes.ECONFLICT) {
			return false
		}
		checkError(code)
		return true
	}

	private fun MemorySegment.deref() = get(C_POINTER, 0)

	private fun MemorySegment.utf8() = getUtf8String(0)

	private fun MemorySegment.utf8OrNull() = if (this == NULL) null else getUtf8String(0)

	private fun Arena.allocate(str: String) = allocateUtf8String(str)

	private fun Arena.allocateInt(i: Int) = allocate(JAVA_INT, i)

	private fun Arena.allocate(strs: List<String>): MemorySegment {
		return allocate(JAVA_LONG.byteSize() * strs.size).also {
			strs.forEachIndexed { index, segment ->
				it.setAtIndex(JAVA_LONG, index.toLong(), allocateUtf8String(segment).address())
			}
		}
	}

	private inline fun Arena.withAllocate(
		layout: MemoryLayout = C_POINTER,
		block: (out: MemorySegment) -> Unit,
	): MemorySegment {
		return allocate(layout).also(block)
	}
}

private object ReturnCodes {
	val OK = GIT_OK()
	val ENOTFOUND = GIT_ENOTFOUND()
	val EUNMERGED = GIT_EUNMERGED()
	val ECONFLICT = GIT_ECONFLICT()
	val EAPPLIED = GIT_EAPPLIED()
	val ITEROVER = GIT_ITEROVER()
}
