package com.mattprecious.stacker.delegates

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use
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

		this.value = Optional.Some(value)
	}
}
