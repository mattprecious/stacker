package com.mattprecious.stacker.delegates

import kotlin.reflect.KProperty

inline fun <reified T : Any?> mutableLazy(noinline initializer: () -> T) = MutableLazy(initializer)

class MutableLazy<T>(private val initializer: () -> T) {
  private var value: Optional<T> = Optional.None

  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
    return when (val v = value) {
      is Optional.Some -> v.value
      Optional.None -> initializer().also { value = Optional.Some(it) }
    }
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this.value = Optional.Some(value)
  }
}
