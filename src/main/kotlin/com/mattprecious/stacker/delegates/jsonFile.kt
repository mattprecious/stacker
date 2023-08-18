package com.mattprecious.stacker.delegates

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okio.buffer
import okio.sink
import okio.source
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.util.EnumSet
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

inline fun <reified T : Any?> jsonFile(
	path: Path,
	permissions: EnumSet<PosixFilePermission>? = null,
	noinline default: (() -> T),
) = JsonFileDelegate(typeOf<T>(), path, permissions, default)

class JsonFileDelegate<T : Any?>(
	type: KType,
	private val path: Path,
	private val permissions: EnumSet<PosixFilePermission>?,
	private val default: () -> T,
) {
	@Suppress("UNCHECKED_CAST")
	private val serializer = Json.serializersModule.serializer(type) as KSerializer<T>
	private var value: Optional<T> = Optional.None

	operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
		return when (val v = value) {
			is Optional.Some -> v.value
			is Optional.None -> {
				val loadedValue = if (path.exists()) {
					val json = path.source().buffer().use { it.readUtf8() }
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
		path.createParentDirectories()
		if (path.notExists()) {
			if (permissions == null) {
				path.createFile()
			} else {
				path.createFile(PosixFilePermissions.asFileAttribute(permissions))
			}
		}

		path.sink().buffer().use { it.writeUtf8(Json.encodeToString(serializer, value)) }

		this.value = Optional.Some(value)
	}
}
