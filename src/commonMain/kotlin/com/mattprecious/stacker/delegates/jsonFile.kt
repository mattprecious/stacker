package com.mattprecious.stacker.delegates

import kotlinx.cinterop.alloc
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
	noinline default: (() -> T),
) = JsonFileDelegate(typeOf<T>(), fs, path, default)

class JsonFileDelegate<T : Any?>(
	type: KType,
	private val fs: FileSystem,
	private val path: Path,
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
					path.requireRestrictiveAccess()
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
		path.setRestrictiveAccess()

		this.value = Optional.Some(value)
	}

	private fun Path.setRestrictiveAccess() {
		// TODO: This won't work on Windows and assumes that the path root is system root.
		chmod(this.toString(), 256.toUShort())
	}

	private fun Path.requireRestrictiveAccess() {
		// TODO: This won't work on Windows and assumes that the path root is system root.
		memScoped {
			val path = this@requireRestrictiveAccess

			val stat = alloc<stat>()
			stat(path.toString(), stat.ptr)

			// The last nine bits are the user, group, and 'other' access permissions, with 3 bits allocated to each.
			// We require the mode to be 400 (octal), which is 256 in decimal.
			check(stat.st_mode.toInt() and 0b111111111 == 256) {
				"User configuration file access is too permissive. Please set file mode of $path to 0400."
			}
		}
	}
}
