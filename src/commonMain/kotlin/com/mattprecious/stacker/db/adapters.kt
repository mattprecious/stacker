package com.mattprecious.stacker.db

import app.cash.sqldelight.ColumnAdapter
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

inline fun <reified T : Any> jsonAdapter() = JsonColumnAdapter<T>(typeOf<T>())

class JsonColumnAdapter<T : Any>(type: KType) : ColumnAdapter<T, String> {
  @Suppress("UNCHECKED_CAST")
  private val serializer = Json.serializersModule.serializer(type) as KSerializer<T>

  override fun decode(databaseValue: String): T {
    return Json.decodeFromString(serializer, databaseValue)
  }

  override fun encode(value: T): String {
    return Json.encodeToString(serializer, value)
  }
}
