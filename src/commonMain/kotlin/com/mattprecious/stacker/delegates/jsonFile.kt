package com.mattprecious.stacker.delegates

import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use
import platform.posix.chmod
import platform.posix.stat
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

inline fun <reified T : Any?> jsonFile(
	fs: FileSystem,
	path: Path,
	permissions: Permissions = Permissions.Default,
	requirePermissionsOnRead: Boolean = false,
	noinline default: (() -> T),
) = JsonFileDelegate(typeOf<T>(), fs, path, permissions, requirePermissionsOnRead, default)

class JsonFileDelegate<T : Any?>(
	type: KType,
	private val fs: FileSystem,
	private val path: Path,
	private val permissions: Permissions,
	private val requirePermissionsOnRead: Boolean,
	private val default: () -> T,
) {
	@Suppress("UNCHECKED_CAST")
	private val serializer = Json.serializersModule.serializer(type) as KSerializer<T>
	private var value: Optional<T> = Optional.None

	init {
		if (requirePermissionsOnRead) {
			require(permissions !is Permissions.Default)
		}
	}

	operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
		return when (val v = value) {
			is Optional.Some -> v.value
			is Optional.None -> {
				val loadedValue = if (fs.exists(path)) {
					path.requirePermissions(permissions)

					val json = fs.source(path).buffer().use { it.readUtf8() }
					Json.decodeFromString(serializer, json)
				} else {
					default()
				}

				value = Optional.Some(loadedValue)

				loadedValue
			}
		}
	}

	operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
		fs.createDirectories(path.parent!!)
		fs.sink(path).buffer().use { it.writeUtf8(Json.encodeToString(serializer, value)) }
		path.setPermissions(permissions)

		this.value = Optional.Some(value)
	}

	private fun Path.setPermissions(permissions: Permissions) {
		when (permissions) {
			Permissions.Default -> {}
			is Permissions.Posix -> {
				// TODO: This won't work on Windows and assumes that the path root is system root.
				chmod(this.toString(), permissions.intValue().convert())
			}
		}

	}

	private fun Path.requirePermissions(permissions: Permissions) {
		if (permissions !is Permissions.Posix) return

		// TODO: This won't work on Windows and assumes that the path root is system root.
		memScoped {
			val path = this@requirePermissions

			val stat = alloc<stat>()
			stat(path.toString(), stat.ptr)

			// The last nine bits are the user, group, and 'other' access permissions, with 3 bits allocated to each.
			// We require the mode to be 400 (octal), which is 256 in decimal.
			check(stat.st_mode.toInt() and 0b111111111 == permissions.intValue()) {
				"User configuration file access is too permissive. Please set file mode of $path to 0400."
			}
		}
	}
}

sealed interface Permissions {
	data object Default : Permissions

	data class Posix(
		val permissions: Set<Permission>,
	) : Permissions {
		fun intValue() = permissions.sumOf { it.value }

		enum class Permission(val value: Int) {
			OwnerRead(1 shl 8),
			OwnerWrite(1 shl 7),
			OwnerExecute(1 shl 6),
			GroupRead(1 shl 5),
			GroupWrite(1 shl 4),
			GroupExecute(1 shl 3),
			OtherRead(1 shl 2),
			OtherWrite(1 shl 1),
			OtherExecute(1),
		}
	}
}
