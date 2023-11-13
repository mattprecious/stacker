package com.mattprecious.stacker.vc

import com.github.git2.GIT_BRANCH_LOCAL
import com.github.git2.GIT_EAPPLIED
import com.github.git2.GIT_ECONFLICT
import com.github.git2.GIT_ENOTFOUND
import com.github.git2.GIT_EUNMERGED
import com.github.git2.GIT_ITEROVER
import com.github.git2.GIT_OK
import com.github.git2.git_branch_iterator
import com.github.git2.git_branch_iterator_free
import com.github.git2.git_branch_iterator_new
import com.github.git2.git_branch_next
import com.github.git2.git_branch_tVar
import com.github.git2.git_buf
import com.github.git2.git_buf_dispose
import com.github.git2.git_error_last
import com.github.git2.git_libgit2_init
import com.github.git2.git_libgit2_shutdown
import com.github.git2.git_oid
import com.github.git2.git_oid_tostr_s
import com.github.git2.git_reference
import com.github.git2.git_reference_name_to_id
import com.github.git2.git_reference_shorthand
import com.github.git2.git_remote
import com.github.git2.git_remote_lookup
import com.github.git2.git_remote_url
import com.github.git2.git_repository
import com.github.git2.git_repository_discover
import com.github.git2.git_repository_free
import com.github.git2.git_repository_head
import com.github.git2.git_repository_open
import com.github.git2.git_repository_path
import com.mattprecious.stacker.shell.Shell
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
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
		get() = logged("configDirectory") { git_repository_path(repo)!!.toKString().toPath() }
	override val currentBranchName: String
		get() = memScoped {
			logged("currentBranchName") { git_reference_shorthand(getHead().pointer)!!.toKString() }
		}
	override val originUrl: String
		get() = memScoped { logged("originUrl") { git_remote_url(getOrigin().pointer)!!.toKString() } }
	override val branches: List<String>
		get() = memScoped {
			logged("branches") {
				val flags = alloc(GIT_BRANCH_LOCAL).ptr
				val iterator = allocPointerTo<git_branch_iterator>()
				checkError(git_branch_iterator_new(iterator.ptr, repo, GIT_BRANCH_LOCAL))
				val list = mapBranches(iterator, flags) { git_reference_shorthand(it.ptr)!!.toKString() }
				git_branch_iterator_free(iterator.value)
				list
			}
		}
	override val editor: String?
		get() = TODO("Not yet implemented")

	private inline fun <T> logged(name: String, block: () -> T): T {
		println("$name start")
		val ret = block()
		println("$name end")
		return ret
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
		logged("close") {
			git_repository_free(repo)
			git_libgit2_shutdown()
		}
	}

	override fun fallthrough(commands: List<String>) {
		TODO("Not yet implemented")
	}

	override fun checkBranches(branchNames: Set<String>): Set<String> {
		return logged("checkBranches") { branchNames.toMutableSet().apply { removeAll(branches.toSet()) } }
	}

	override fun checkout(branchName: String) {
		TODO("Not yet implemented")
	}

	override fun createBranchFromCurrent(branchName: String) {
		TODO("Not yet implemented")
	}

	override fun renameBranch(branchName: String, newName: String) {
		TODO("Not yet implemented")
	}

	override fun delete(branchName: String) {
		TODO("Not yet implemented")
	}

	override fun pushBranches(branchNames: List<String>) {
		TODO("Not yet implemented")
	}

	override fun pull(branchName: String) {
		TODO("Not yet implemented")
	}

	override fun latestCommitInfo(branchName: String): VersionControl.CommitInfo {
		TODO("Not yet implemented")
	}

	override fun isAncestor(branchName: String, possibleAncestorName: String): Boolean {
		TODO("Not yet implemented")
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

	private fun checkError(result: Int) {
		check(result == ReturnCodes.OK) {
			val message = git_error_last()!!.pointed.message!!.toKString()
			"Exit code: $result\n$message"
		}
	}

	/** Convert a branch name to a refspec. */
	private fun String.asBranchRevSpec() = "refs/heads/$this"

	/** @return git_oid* */
	private fun MemScope.getCommitId(branchName: String): git_oid {
		logged("getCommitId") {
			val oid = alloc<git_oid>()
			println("sup")
			checkError(git_reference_name_to_id(oid.ptr, repo, branchName.asBranchRevSpec()))
			println("yo")
			return oid
		}
	}

	/** @return git_reference* */
	private fun MemScope.getHead(): git_reference {
		return logged("getHead") {
			val head = allocPointerTo<git_reference>()
			checkError(git_repository_head(head.ptr, repo))
			head.pointed!!
		}
	}

	/** @return git_remote*, should be freed with [git_remote_free]. */
	private fun MemScope.getOrigin(): git_remote {
		logged("getOrigin") {
			val remote = allocPointerTo<git_remote>()
			checkError(git_remote_lookup(remote.ptr, repo, "origin"))
			return remote.pointed!!
		}
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
		return logged("mapBranches") {
			buildList {
				forEachBranch(iterator, flags) { add(transform(it)) }
			}
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
		logged("forEachBranch") {
			while (true) {
				val reference = allocPointerTo<git_reference>()
				val code = git_branch_next(reference.pointer, flags, iterator.value)
				if (code == ReturnCodes.ITEROVER) return

				action(reference.pointed!!)
			}
		}
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
