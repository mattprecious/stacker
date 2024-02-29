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
	createPermissions: Permissions = Permissions.Default,
	maximumAllowedPermissions: Permissions.Posix? = null,
	noinline default: (() -> T),
) = JsonFileDelegate(typeOf<T>(), fs, path, createPermissions, maximumAllowedPermissions, default)

class JsonFileDelegate<T : Any?>(
	type: KType,
	private val fs: FileSystem,
	private val path: Path,
	private val createPermissions: Permissions,
	private val maximumAllowedPermissions: Permissions.Posix?,
	private val default: () -> T,
) {
	@Suppress("UNCHECKED_CAST")
	private val serializer = Json.serializersModule.serializer(type) as KSerializer<T>
	private var value: Optional<T> = Optional.None

	operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
		return when (val v = value) {
			is Optional.Some -> v.value
			is Optional.None -> {
				val loadedValue = if (fs.exists(path)) {
					if (maximumAllowedPermissions != null) {
						path.requirePermissions(maximumAllowedPermissions)
					}

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
		path.setPermissions(createPermissions)

		this.value = Optional.Some(value)
	}

	private fun Path.setPermissions(permissions: Permissions) {
		when (permissions) {
			Permissions.Default -> {}
			is Permissions.Posix -> {
				// TODO: This won't work on Windows and assumes that the path root is system root.
				chmod(this.toString(), permissions.value().intValue.convert())
			}
		}
	}

	private fun Path.requirePermissions(permissions: Permissions.Posix) {
		// TODO: This won't work on Windows and assumes that the path root is system root.
		memScoped {
			val path = this@requirePermissions

			val stat = alloc<stat>()
			stat(path.toString(), stat.ptr)

			val maximumPermissions = permissions.value()
			val pathPermissions = PosixPermissionsInt(stat.st_mode.toInt())

			check(
				pathPermissions.ownerValue <= maximumPermissions.ownerValue &&
					pathPermissions.groupValue <= maximumPermissions.groupValue &&
					pathPermissions.otherValue <= maximumPermissions.otherValue,
			) {
				"User configuration file access is too permissive. Please set file mode of $path to be no greater than " +
					"'${maximumPermissions.intValue.toString(8)}'."
			}
		}
	}
}

value class PosixPermissionsInt(val intValue: Int) {
	val ownerValue: Int
		get() = intValue shr 6 and 0b111
	val groupValue: Int
		get() = intValue shr 3 and 0b111
	val otherValue: Int
		get() = intValue and 0b111
}

sealed interface Permissions {
	data object Default : Permissions

	data class Posix(
		val permissions: Set<Permission>,
	) : Permissions {
		fun value() = PosixPermissionsInt(permissions.sumOf { it.value })

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
