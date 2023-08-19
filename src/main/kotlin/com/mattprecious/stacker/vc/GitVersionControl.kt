package com.mattprecious.stacker.vc

import com.github.git2_h.C_POINTER
import com.github.git2_h.GIT_BRANCH_LOCAL
import com.github.git2_h.GIT_CHECKOUT_OPTIONS_VERSION
import com.github.git2_h.GIT_EAPPLIED
import com.github.git2_h.GIT_ENOTFOUND
import com.github.git2_h.GIT_OK
import com.github.git2_h.GIT_PUSH_OPTIONS_VERSION
import com.github.git2_h.GIT_REBASE_OPERATION_PICK
import com.github.git2_h.GIT_REMOTE_CALLBACKS_VERSION
import com.github.git2_h.git_annotated_commit_from_revspec
import com.github.git2_h.git_annotated_commit_lookup
import com.github.git2_h.git_branch_create
import com.github.git2_h.git_branch_iterator_free
import com.github.git2_h.git_branch_iterator_new
import com.github.git2_h.git_branch_move
import com.github.git2_h.git_branch_next
import com.github.git2_h.git_buf_dispose
import com.github.git2_h.git_commit_body
import com.github.git2_h.git_error_last
import com.github.git2_h.git_merge_base
import com.github.git2_h.git_oid_equal
import com.github.git2_h.git_oid_fromstr
import com.github.git2_h.git_oid_tostr_s
import com.github.git2_h.git_push_options_init
import com.github.git2_h.git_rebase_commit
import com.github.git2_h.git_rebase_free
import com.github.git2_h.git_rebase_next
import com.github.git2_h.git_rebase_operation_byindex
import com.github.git2_h.git_remote_init_callbacks
import com.github.git2_h.git_remote_lookup
import com.github.git2_h.git_repository_discover
import com.github.git2_h.git_repository_free
import com.github.git2_h.git_repository_head
import com.github.git2_h.git_repository_open
import com.github.git2_h.git_repository_path
import com.github.git2_h.git_signature_default
import com.github.git2_h.git_signature_free
import com.github.git2_h_1.GIT_ITEROVER
import com.github.git2_h_1.git_checkout_options_init
import com.github.git2_h_1.git_checkout_tree
import com.github.git2_h_1.git_commit_lookup
import com.github.git2_h_1.git_commit_summary
import com.github.git2_h_1.git_credential_ssh_key_from_agent
import com.github.git2_h_1.git_rebase_abort
import com.github.git2_h_1.git_rebase_finish
import com.github.git2_h_1.git_rebase_init
import com.github.git2_h_1.git_rebase_open
import com.github.git2_h_1.git_rebase_operation_current
import com.github.git2_h_1.git_reference_lookup
import com.github.git2_h_1.git_reference_name_to_id
import com.github.git2_h_1.git_reference_set_target
import com.github.git2_h_1.git_reference_shorthand
import com.github.git2_h_1.git_remote_push
import com.github.git2_h_1.git_remote_url
import com.github.git2_h_1.git_repository_set_head
import com.github.git2_h_1.git_revparse_single
import com.github.git2_h_2.GIT_REBASE_NO_OPERATION
import com.github.git2_h_2.git_libgit2_init
import com.github.git2_h_2.git_libgit2_shutdown
import com.github.git_buf
import com.github.git_checkout_options
import com.github.git_credential
import com.github.git_credential_acquire_cb
import com.github.git_error
import com.github.git_oid
import com.github.git_push_options
import com.github.git_rebase_operation
import com.github.git_remote_callbacks
import com.github.git_strarray
import com.github.git_transport_certificate_check_cb
import com.mattprecious.stacker.shell.Shell
import com.mattprecious.stacker.stack.Branch
import com.mattprecious.stacker.vc.VersionControl.CommitInfo
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySegment.NULL
import java.lang.foreign.ValueLayout
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.nio.file.Path

class GitVersionControl(
	arena: Arena,
	private val shell: Shell,
) : VersionControl {
	private val repo: MemorySegment

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
			val list = iterator.mapBranches(flags) { git_reference_shorthand(it).utf8() }
			git_branch_iterator_free(iterator)
			return@arena list
		}

	init {
		loadLibGit2()
		git_libgit2_init()

		val currentPath = arena.allocate(System.getProperty("user.dir"))

		val repoPathBuf = arena.withAllocate(git_buf.`$LAYOUT`()) {
			checkError(git_repository_discover(it, currentPath, 0, NULL))
		}

		repo = arena.withAllocate {
			val repoPath = git_buf.`ptr$get`(repoPathBuf)
			checkError(git_repository_open(it, repoPath))
		}.deref()

		git_buf_dispose(repoPathBuf)
	}

	override fun close() {
		// Not sure if this is needed since the process is being killed.
		git_repository_free(repo)
		git_libgit2_shutdown()
	}

	override fun fallthrough(commands: List<String>) {
		shell.exec("git", *commands.toTypedArray())
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

	override fun renameBranch(branch: Branch, newName: String): Unit = arena {
		withAllocate { checkError(git_branch_move(it, getBranch(branch.name), allocate(newName), 0)) }
	}

	override fun latestCommitInfo(branch: Branch): CommitInfo = arena {
		val commit = getCommitForBranch(branch.name)
		val title = git_commit_summary(commit).utf8()
		val body = git_commit_body(commit).utf8OrNull()

		return@arena CommitInfo(
			title = title,
			body = body,
		)
	}

	override fun isAncestor(branchName: String, possibleAncestor: Branch): Boolean = arena {
		val branchCommitId = getCommitId(branchName)
		val possibleAncestorCommitId = getCommitId(possibleAncestor.name)

		val base = withAllocate(git_oid.`$LAYOUT`()) {
			val code = git_merge_base(it, repo, possibleAncestorCommitId, branchCommitId)
			if (code == ReturnCodes.ENOTFOUND) return@arena false
			checkError(code)
		}

		return@arena git_oid_equal(base, possibleAncestorCommitId) == 1
	}

	override fun needsRestack(branch: Branch): Boolean {
		val parent = branch.parent ?: return false
		val parentSha = getSha(parent.name)
		return branch.parentSha != parentSha || !isAncestor(branch.name, parent)
	}

	override fun restack(branch: Branch) = arena {
		val branchCommit = getAnnotatedCommit(branch.name)
		val ontoCommit = getAnnotatedCommit(branch.parent!!.name)

		val upstreamId =
			withAllocate(git_oid.`$LAYOUT`()) { checkError(git_oid_fromstr(it, allocate(branch.parentSha!!))) }
		val upstreamCommit = withAllocate { checkError(git_annotated_commit_lookup(it, repo, upstreamId)) }.deref()

		val rebase = withAllocate {
			checkError(git_rebase_init(it, repo, branchCommit, upstreamCommit, ontoCommit, NULL))
		}.deref()

		rebase.performRebase(branch.name)
		git_rebase_free(rebase)
	}

	override fun getSha(branch: String): String = arena {
		return@arena git_oid_tostr_s(getCommitId(branch)).utf8()
	}

	override fun abortRebase() = arena {
		val rebase = getRebase() ?: return@arena
		checkError(git_rebase_abort(rebase))
		git_rebase_free(rebase)
	}

	override fun continueRebase(branchName: String) = arena {
		val rebase = getRebase() ?: return@arena
		rebase.performRebase(branchName)
		git_rebase_free(rebase)
	}

	override fun pushBranches(branches: List<Branch>): Unit = arena {
		val strings = allocate(branches.map { "+${it.name.asBranchRevSpec()}:${it.name.asBranchRevSpec()}" })

		val refs = allocate(git_strarray.`$LAYOUT`())
		git_strarray.`count$set`(refs, branches.size.toLong())
		git_strarray.`strings$set`(refs, strings)

		val acquireCredentialCb = git_credential_acquire_cb { out, _, username, _, _ ->
			val credentials = withAllocate(git_credential.`$LAYOUT`()) {
				checkError(git_credential_ssh_key_from_agent(it, username))
			}

			out.copyFrom(credentials)
			return@git_credential_acquire_cb 0
		}
		val acquireCredential = git_credential_acquire_cb.allocate(acquireCredentialCb, scope())

		// Is this bad? I don't know why it's not a known host.
		val certificateCheckCb = git_transport_certificate_check_cb { _, valid, _, _ -> valid }
		val certificateCheck = git_transport_certificate_check_cb.allocate(certificateCheckCb, scope())

		val callbacks = withAllocate(git_remote_callbacks.`$LAYOUT`()) {
			checkError(git_remote_init_callbacks(it, GIT_REMOTE_CALLBACKS_VERSION()))
		}
		git_remote_callbacks.`credentials$set`(callbacks, acquireCredential)
		git_remote_callbacks.`certificate_check$set`(callbacks, certificateCheck)

		// TODO: Atomic? I don't think libgit2 supports this.
		val options = withAllocate(git_push_options.`$LAYOUT`()) {
			checkError(git_push_options_init(it, GIT_PUSH_OPTIONS_VERSION()))
		}

		git_push_options.`callbacks$slice`(options).copyFrom(callbacks)

		checkError(git_remote_push(getOrigin(), refs, options))
	}

	private fun <T> arena(block: Arena.() -> T) = Arena.openConfined().use(block)

	private fun checkError(result: Int) {
		check(result == ReturnCodes.OK) {
			val message = git_error.`message$get`(git_error_last()).utf8()
			"Exit code: $result\n$message"
		}
	}

	/** Convert a branch name to a refspec. */
	private fun String.asBranchRevSpec() = "refs/heads/$this"

	/** @return git_oid* */
	context(Arena)
	private fun getCommitId(branchName: String): MemorySegment {
		return withAllocate(git_oid.`$LAYOUT`()) {
			checkError(git_reference_name_to_id(it, repo, allocate(branchName.asBranchRevSpec())))
		}
	}

	/** @return git_commit* */
	context(Arena)
	private fun getCommitForBranch(branchName: String): MemorySegment {
		return withAllocate { checkError(git_commit_lookup(it, repo, getCommitId(branchName))) }.deref()
	}

	/** @return git_reference* */
	context(Arena)
	private fun getHead(): MemorySegment {
		return withAllocate { checkError(git_repository_head(it, repo)) }.deref()
	}

	context(Arena)
	private fun getBranch(branchName: String): MemorySegment {
		return withAllocate { checkError(git_reference_lookup(it, repo, allocate(branchName.asBranchRevSpec()))) }.deref()
	}

	/** @return git_remote* */
	context(Arena)
	private fun getOrigin(): MemorySegment {
		return withAllocate { checkError(git_remote_lookup(it, repo, allocate("origin"))) }.deref()
	}

	/** @param treeish git_commit* or git_object* */
	context(Arena)
	private fun checkout(branchName: String, treeish: MemorySegment) {
		val options = withAllocate(git_checkout_options.`$LAYOUT`()) {
			git_checkout_options_init(it, GIT_CHECKOUT_OPTIONS_VERSION())
		}

		checkError(git_checkout_tree(repo, treeish, options))
		checkError(git_repository_set_head(repo, allocate(branchName.asBranchRevSpec())))
	}

	/**
	 * @receiver git_branch_iterator*
	 * @param flags int, one of GIT_BRANCH_*.
	 * @param transform git_reference*
	 */
	context(Arena)
	private fun <T> MemorySegment.mapBranches(
		flags: MemorySegment,
		transform: (branch: MemorySegment) -> T,
	): List<T> {
		return buildList {
			forEachBranch(flags) { add(transform(it)) }
		}
	}

	/**
	 * @receiver git_branch_iterator*
	 * @param flags int, one of GIT_BRANCH_*.
	 * @param action git_reference*
	 */
	context(Arena)
	private fun MemorySegment.forEachBranch(
		flags: MemorySegment,
		action: (branch: MemorySegment) -> Unit,
	) {
		while (true) {
			val branch = withAllocate {
				val code = git_branch_next(it, flags, this)
				if (code == ReturnCodes.ITEROVER) return
			}.deref()

			action(branch)
		}
	}

	/** @return git_annotated_commit* */
	context(Arena)
	private fun getAnnotatedCommit(branchName: String): MemorySegment {
		return withAllocate {
			checkError(git_annotated_commit_from_revspec(it, repo, allocate(branchName.asBranchRevSpec())))
		}.deref()
	}

	/** @return git_rebase* */
	context(Arena)
	private fun getRebase(): MemorySegment? {
		return withAllocate {
			val code = git_rebase_open(it, repo, NULL)
			if (code == ReturnCodes.ENOTFOUND) return null
			checkError(code)
		}.deref()
	}

	/** @receiver git_rebase* */
	context(Arena)
	private fun MemorySegment.performRebase(branchName: String) {
		val signature = withAllocate { checkError(git_signature_default(it, repo)) }.deref()

		forEachRebaseOperation {
			performRebaseOperation(
				operation = it,
				committer = signature,
			)
		}

		checkError(git_rebase_finish(this, signature))
		git_signature_free(signature)

		val headId = withAllocate(git_oid.`$LAYOUT`()) {
			checkError(git_reference_name_to_id(it, repo, allocate("HEAD")))
		}

		checkError(git_reference_set_target(allocate(C_POINTER), getBranch(branchName), headId, NULL))
	}

	/**
	 * @receiver git_rebase*
	 *
	 * @param action git_rebase_operation*
	 */
	context(Arena)
	private inline fun MemorySegment.forEachRebaseOperation(
		action: (operation: MemorySegment) -> Unit,
	) {
		val operation = allocate(C_POINTER)

		val operationIndex = git_rebase_operation_current(this)
		if (operationIndex != GIT_REBASE_NO_OPERATION()) {
			action(git_rebase_operation_byindex(this, operationIndex))
		}

		while (true) {
			val code = git_rebase_next(operation, this)
			if (code == ReturnCodes.ITEROVER) break
			checkError(code)
			action(operation.deref())
		}
	}

	/**
	 * @receiver git_rebase*
	 *
	 * @param operation git_rebase_operation*
	 * @param committer git_signature*
	 */
	context(Arena)
	private fun MemorySegment.performRebaseOperation(
		operation: MemorySegment,
		committer: MemorySegment,
	) {
		val operationType = git_rebase_operation.`type$get`(operation)
		check(operationType == GIT_REBASE_OPERATION_PICK())
		val code = git_rebase_commit(allocate(git_oid.`$LAYOUT`()), this, NULL, committer, NULL, NULL)
		if (code != ReturnCodes.EAPPLIED) checkError(code)
	}

	private fun MemorySegment.deref() = get(C_POINTER, 0)

	private fun MemorySegment.utf8() = getUtf8String(0)

	private fun MemorySegment.utf8OrNull() = if (this == NULL) null else getUtf8String(0)

	private fun Arena.allocate(str: String) = allocateUtf8String(str)

	context(Arena)
	private fun allocateInt(i: Int) = allocate(ValueLayout.JAVA_INT, i)

	context(Arena)
	private fun allocate(strs: List<String>): MemorySegment {
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
	val EAPPLIED = GIT_EAPPLIED()
	val ITEROVER = GIT_ITEROVER()
}
