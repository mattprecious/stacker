package com.mattprecious.stacker.vc

import com.github.git2.GIT_BRANCH_LOCAL
import com.github.git2.GIT_CHECKOUT_OPTIONS_VERSION
import com.github.git2.GIT_CREDTYPE_SSH_KEY
import com.github.git2.GIT_EAPPLIED
import com.github.git2.GIT_ECONFLICT
import com.github.git2.GIT_ENOTFOUND
import com.github.git2.GIT_EUNMERGED
import com.github.git2.GIT_FETCH_OPTIONS_VERSION
import com.github.git2.GIT_ITEROVER
import com.github.git2.GIT_MERGE_ANALYSIS_FASTFORWARD
import com.github.git2.GIT_MERGE_ANALYSIS_UP_TO_DATE
import com.github.git2.GIT_MERGE_PREFERENCE_NO_FASTFORWARD
import com.github.git2.GIT_OK
import com.github.git2.GIT_PUSH_OPTIONS_VERSION
import com.github.git2.GIT_REMOTE_CALLBACKS_VERSION
import com.github.git2.git_annotated_commit
import com.github.git2.git_annotated_commit_from_revspec
import com.github.git2.git_annotated_commit_id
import com.github.git2.git_branch_create
import com.github.git2.git_branch_delete
import com.github.git2.git_branch_iterator
import com.github.git2.git_branch_iterator_free
import com.github.git2.git_branch_iterator_new
import com.github.git2.git_branch_move
import com.github.git2.git_branch_next
import com.github.git2.git_branch_tVar
import com.github.git2.git_buf
import com.github.git2.git_buf_dispose
import com.github.git2.git_checkout_options
import com.github.git2.git_checkout_options_init
import com.github.git2.git_checkout_tree
import com.github.git2.git_commit
import com.github.git2.git_commit_body
import com.github.git2.git_commit_lookup
import com.github.git2.git_commit_summary
import com.github.git2.git_config
import com.github.git2.git_config_free
import com.github.git2.git_config_get_string
import com.github.git2.git_config_snapshot
import com.github.git2.git_credential_ssh_key_from_agent
import com.github.git2.git_error_last
import com.github.git2.git_fetch_options
import com.github.git2.git_fetch_options_init
import com.github.git2.git_libgit2_init
import com.github.git2.git_libgit2_shutdown
import com.github.git2.git_merge_analysis_for_ref
import com.github.git2.git_merge_analysis_tVar
import com.github.git2.git_merge_base
import com.github.git2.git_merge_preference_tVar
import com.github.git2.git_object
import com.github.git2.git_oid
import com.github.git2.git_oid_equal
import com.github.git2.git_oid_tostr_s
import com.github.git2.git_push_options
import com.github.git2.git_push_options_init
import com.github.git2.git_reference
import com.github.git2.git_reference_free
import com.github.git2.git_reference_lookup
import com.github.git2.git_reference_name_to_id
import com.github.git2.git_reference_set_target
import com.github.git2.git_reference_shorthand
import com.github.git2.git_remote
import com.github.git2.git_remote_callbacks
import com.github.git2.git_remote_fetch
import com.github.git2.git_remote_free
import com.github.git2.git_remote_init_callbacks
import com.github.git2.git_remote_lookup
import com.github.git2.git_remote_push
import com.github.git2.git_remote_url
import com.github.git2.git_repository
import com.github.git2.git_repository_config
import com.github.git2.git_repository_discover
import com.github.git2.git_repository_free
import com.github.git2.git_repository_head
import com.github.git2.git_repository_open
import com.github.git2.git_repository_path
import com.github.git2.git_repository_set_head
import com.github.git2.git_revparse_single
import com.github.git2.git_strarray
import com.mattprecious.stacker.shell.Shell
import com.mattprecious.stacker.vc.VersionControl.CommitInfo
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCStringArray
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlinx.cinterop.ptr as pointer

class GitVersionControl(
	private val scope: MemScope,
	private val fs: FileSystem,
	private val shell: Shell,
) : VersionControl {
	private val nullableRepo: CPointerVar<git_repository>?
	private val repo: CPointer<git_repository>
		get() = nullableRepo!!.value!!

	override val repoDiscovered: Boolean
		get() = nullableRepo != null
	override val configDirectory: Path
		get() = git_repository_path(repo)!!.toKString().toPath()
	override val currentBranchName: String
		get() = memScoped { git_reference_shorthand(getHead().pointer)!!.toKString() }
	override val originUrl: String
		get() = memScoped { git_remote_url(getOrigin().pointer)!!.toKString() }
	override val branches: List<String>
		get() = memScoped {
			val flags = alloc(GIT_BRANCH_LOCAL).ptr
			val iterator = allocPointerTo<git_branch_iterator>()
			checkError(git_branch_iterator_new(iterator.ptr, repo, GIT_BRANCH_LOCAL))
			val list = mapBranches(iterator, flags) { git_reference_shorthand(it.ptr)!!.toKString() }
			git_branch_iterator_free(iterator.value)
			list
		}
	override val editor: String?
		get() = memScoped {
			val config = allocPointerTo<git_config>()
			checkError(git_repository_config(config.ptr, repo))

			val configSnapshot = allocPointerTo<git_config>()
			checkError(git_config_snapshot(configSnapshot.ptr, config.value))

			return@memScoped try {
				val editor = allocPointerTo<ByteVar>()
				val code = git_config_get_string(editor.ptr, configSnapshot.value, "core.editor")
				if (code == ReturnCodes.ENOTFOUND) return@memScoped null
				checkError(code)

				editor.value!!.toKString()
			} finally {
				git_config_free(configSnapshot.value)
				git_config_free(config.value)
			}
		}

	init {
		git_libgit2_init()

		val repoPathBuf = with(scope) { repoPath() }

		nullableRepo = if (repoPathBuf == null) {
			null
		} else {
			val repo = scope.allocPointerTo<git_repository>()
			checkError(git_repository_open(repo.ptr, repoPathBuf))
			repo
		}
	}

	/** @return git_buf */
	private fun MemScope.repoPath(): String? {
		val currentPath = fs.canonicalize(".".toPath())
		val buf = alloc<git_buf>()
		val code = git_repository_discover(buf.pointer, currentPath.toString(), 0, null)
		if (code == ReturnCodes.ENOTFOUND) return null
		checkError(code)
		val something = buf.ptr!!.toKString()
		git_buf_dispose(buf.pointer)
		return something
	}

	override fun close() {
		git_repository_free(repo)
		git_libgit2_shutdown()
	}

	override fun fallthrough(commands: List<String>) {
		shell.exec("git", *commands.toTypedArray())
	}

	override fun checkBranches(branchNames: Set<String>): Set<String> {
		return branchNames.toMutableSet().apply { removeAll(branches.toSet()) }
	}

	override fun checkout(branchName: String) = memScoped {
		val treeish = allocPointerTo<git_object>()
		checkError(git_revparse_single(treeish.ptr, repo, branchName))
		checkout(branchName, treeish.pointed!!)
	}

	override fun createBranchFromCurrent(branchName: String) = memScoped {
		val commit = getCommitForBranch(currentBranchName)
		checkError(git_branch_create(allocPointerTo<git_reference>().ptr, repo, branchName, commit.ptr, 0))
		checkout(branchName, commit.asObject())
	}

	override fun renameBranch(branchName: String, newName: String) = memScoped {
		checkError(git_branch_move(allocPointerTo<git_reference>().ptr, getBranch(branchName).ptr, newName, 0))
	}

	override fun delete(branchName: String) = memScoped {
		checkError(git_branch_delete(getBranch(branchName).ptr))
	}

	override fun latestCommitInfo(branchName: String): CommitInfo = memScoped {
		val commit = getCommitForBranch(branchName)
		val title = git_commit_summary(commit.ptr)!!.toKString()
		val body = git_commit_body(commit.ptr)?.toKString()

		return CommitInfo(
			title = title,
			body = body,
		)
	}

	override fun isAncestor(branchName: String, possibleAncestorName: String): Boolean = memScoped {
		val branchCommitId = getCommitId(branchName)
		val possibleAncestorCommitId = getCommitId(possibleAncestorName)

		val oid = alloc<git_oid>()
		val code = git_merge_base(oid.ptr, repo, possibleAncestorCommitId.ptr, branchCommitId.ptr)
		if (code == ReturnCodes.ENOTFOUND) return false
		checkError(code)

		return@memScoped git_oid_equal(oid.ptr, possibleAncestorCommitId.ptr) == 1
	}

	override fun restack(branchName: String, parentName: String, parentSha: String): Boolean {
		TODO("Not yet implemented")
	}

	override fun getSha(branch: String): String = memScoped {
		return@memScoped git_oid_tostr_s(getCommitId(branch).ptr)!!.toKString()
	}

	override fun abortRebase() {
		TODO("Not yet implemented")
	}

	override fun continueRebase(branchName: String): Boolean {
		TODO("Not yet implemented")
	}

	override fun pushBranches(branchNames: List<String>): Unit = memScoped {
		// Libgit2 auth doesn't work on enterprise repos for some reason. Fall back to shell command for now.
		shell.exec("git", "push", "-f", "--atomic", "origin", *branchNames.toTypedArray())
	}

	private fun pushBranchesLibGit(branchNames: List<String>): Unit = memScoped {
		val refs = alloc<git_strarray>()
		refs.count = branchNames.size.toULong()
		refs.strings = branchNames.map { it.asBranchRevSpec() }.map { "+$it:$it" }.toCStringArray(this)

		// TODO: Atomic? I don't think libgit2 supports this.
		val options = alloc<git_push_options>()
		checkError(git_push_options_init(options.ptr, GIT_PUSH_OPTIONS_VERSION.toUInt()))
		populateRemoteCallbacks(options.callbacks)

		val origin = getOrigin()
		checkError(git_remote_push(origin.ptr, refs.ptr, options.ptr))
		git_remote_free(origin.ptr)
	}

	override fun pull(branchName: String) {
		// Libgit2 auth doesn't work on enterprise repos for some reason. Fall back to shell command for now.
		val currentBranch = currentBranchName
		checkout(branchName)
		shell.exec("git", "pull", "origin", branchName)
		checkout(currentBranch)
	}

	private fun pullLibGit(branchName: String): Unit = memScoped {
		val refs = alloc<git_strarray>()
		refs.count = 1.toULong()
		refs.strings = listOf(branchName).toCStringArray(this)

		val pullOptions = alloc<git_fetch_options>()
		checkError(git_fetch_options_init(pullOptions.ptr, GIT_FETCH_OPTIONS_VERSION.toUInt()))
		populateRemoteCallbacks(pullOptions.callbacks)

		val origin = getOrigin()
		checkError(git_remote_fetch(origin.ptr, refs.ptr, pullOptions.ptr, null))
		git_remote_free(origin.ptr)

		val head = allocPointerTo<git_annotated_commit>()
		checkError(git_annotated_commit_from_revspec(head.ptr, repo, branchName.asRemoteBranchRevSpec()))

		val analysis = getMergeAnalysis(getBranch(branchName), head)
		if (analysis.upToDate) {
			return
		}

		check(analysis.fastForward && !analysis.preferenceNoFastForward) {
			"$branchName cannot be fast-forwarded."
		}

		val commitId = git_annotated_commit_id(head.value)!!.pointed
		if (currentBranchName == branchName) {
			checkoutTree(commitId.reinterpret())
		}

		setBranchTarget(branchName, commitId)
	}

	private fun populateRemoteCallbacks(callbacks: git_remote_callbacks) {
		checkError(git_remote_init_callbacks(callbacks.ptr, GIT_REMOTE_CALLBACKS_VERSION.toUInt()))
		callbacks.credentials = staticCFunction { out, _, username, types, _ ->
			check(types.toInt() and GIT_CREDTYPE_SSH_KEY == GIT_CREDTYPE_SSH_KEY) {
				"Unsupported credential types: $types"
			}

			checkError(git_credential_ssh_key_from_agent(out, username?.toKString()))

			return@staticCFunction 0
		}

		// Is this bad? I don't know why it's not a known host.
		callbacks.certificate_check = staticCFunction { _, valid, _, _ -> valid }
	}

	/** Convert a branch name to a refspec. */
	private fun String.asBranchRevSpec() = "refs/heads/$this"

	/** Convert a branch name to a remote refspec. */
	private fun String.asRemoteBranchRevSpec() = "refs/remotes/origin/$this"

	private fun MemScope.getCommitId(branchName: String): git_oid {
		val oid = alloc<git_oid>()
		checkError(git_reference_name_to_id(oid.ptr, repo, branchName.asBranchRevSpec()))
		return oid
	}

	private fun MemScope.getCommitForBranch(branchName: String): git_commit {
		val commit = allocPointerTo<git_commit>()
		checkError(git_commit_lookup(commit.pointer, repo, getCommitId(branchName).pointer))
		return commit.pointed!!
	}

	private fun MemScope.getHead(): git_reference {
		val head = allocPointerTo<git_reference>()
		checkError(git_repository_head(head.ptr, repo))
		return head.pointed!!
	}

	private fun MemScope.getBranch(branchName: String): git_reference {
		val ref = allocPointerTo<git_reference>()
		checkError(git_reference_lookup(ref.ptr, repo, branchName.asBranchRevSpec()))
		return ref.pointed!!
	}

	/** Should be freed with [git_remote_free]. */
	private fun MemScope.getOrigin(): git_remote {
		val remote = allocPointerTo<git_remote>()
		checkError(git_remote_lookup(remote.ptr, repo, "origin"))
		return remote.pointed!!
	}

	/** @param treeish git_commit* or git_object* */
	private fun MemScope.checkout(
		branchName: String,
		treeish: git_object,
	) {
		checkoutTree(treeish)
		checkError(git_repository_set_head(repo, branchName.asBranchRevSpec()))
	}

	private fun MemScope.checkoutTree(treeish: git_object) {
		val options = alloc<git_checkout_options>()
		git_checkout_options_init(options.ptr, GIT_CHECKOUT_OPTIONS_VERSION.toUInt())

		checkError(git_checkout_tree(repo, treeish.ptr, options.ptr))
	}

	private data class MergeAnalysis(
		private val analysisFlags: Int,
		private val mergePreferenceFlags: Int,
	) {
		val upToDate = analysisFlags and GIT_MERGE_ANALYSIS_UP_TO_DATE.toInt() != 0
		val fastForward = analysisFlags and GIT_MERGE_ANALYSIS_FASTFORWARD.toInt() != 0
		val preferenceNoFastForward = mergePreferenceFlags and GIT_MERGE_PREFERENCE_NO_FASTFORWARD.toInt() != 0
	}

	/**
	 * @param into git_reference*
	 * @param commit git_annotated_commit*
	 */
	private fun getMergeAnalysis(
		into: git_reference,
		commit: CPointerVar<git_annotated_commit>,
	): MergeAnalysis = memScoped {
		val analysis = alloc<git_merge_analysis_tVar>()
		val mergePreference = alloc<git_merge_preference_tVar>()

		checkError(git_merge_analysis_for_ref(analysis.ptr, mergePreference.ptr, repo, into.ptr, commit.ptr, 1.toULong()))

		return@memScoped MergeAnalysis(
			analysisFlags = analysis.value.toInt(),
			mergePreferenceFlags = mergePreference.value.toInt(),
		)
	}

	/**
	 * @receiver git_branch_iterator*
	 * @param flags int, one of GIT_BRANCH_*.
	 * @param transform git_reference*
	 */
	private fun <T> MemScope.mapBranches(
		iterator: CPointerVar<git_branch_iterator>,
		flags: CPointer<git_branch_tVar>,
		transform: (branch: git_reference) -> T,
	): List<T> {
		return buildList {
			forEachBranch(iterator, flags) { add(transform(it)) }
		}
	}

	/**
	 * @receiver git_branch_iterator*
	 * @param flags int, one of GIT_BRANCH_*.
	 * @param action git_reference*
	 */
	private fun MemScope.forEachBranch(
		iterator: CPointerVar<git_branch_iterator>,
		flags: CPointer<git_branch_tVar>,
		action: (branch: git_reference) -> Unit,
	) {
		while (true) {
			val reference = allocPointerTo<git_reference>()
			val code = git_branch_next(reference.pointer, flags, iterator.value)
			if (code == ReturnCodes.ITEROVER) return

			action(reference.pointed!!)
		}
	}

	private fun MemScope.setBranchTarget(
		branchName: String,
		target: git_oid,
	) {
		val ref = allocPointerTo<git_reference>()
		checkError(git_reference_set_target(ref.ptr, getBranch(branchName).ptr, target.ptr, null))
		git_reference_free(ref.value)
	}

	private fun git_commit.asObject(): git_object {
		// All commits are objects, so it's safe to do this.
		return reinterpret()
	}
}

// This is invoked from inside C callbacks so it needs to be static.
private fun checkError(result: Int) {
	check(result == ReturnCodes.OK) {
		val message = git_error_last()!!.pointed.message!!.toKString()
		"Exit code: $result\n$message"
	}
}

private object ReturnCodes {
	val OK = GIT_OK
	val ENOTFOUND = GIT_ENOTFOUND
	val EUNMERGED = GIT_EUNMERGED
	val ECONFLICT = GIT_ECONFLICT
	val EAPPLIED = GIT_EAPPLIED
	val ITEROVER = GIT_ITEROVER
}
