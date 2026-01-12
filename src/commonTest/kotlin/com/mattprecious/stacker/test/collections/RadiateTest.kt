package com.mattprecious.stacker.test.collections

import assertk.assertThat
import assertk.assertions.containsExactly
import com.mattprecious.stacker.collections.radiateFrom
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RadiateTest {
  @Test
  fun test() {
    // Does not throw until accessed.
    listOf<String>().radiateFrom(0)
    assertFailsWith<IndexOutOfBoundsException> { listOf<String>().radiateFrom(0).toList() }
    assertFailsWith<IndexOutOfBoundsException> { listOf("a").radiateFrom(-1).toList() }
    assertFailsWith<IndexOutOfBoundsException> { listOf("a").radiateFrom(1).toList() }

    assertThat(listOf("a").radiateFrom(0)).containsExactly("a")
    assertThat(listOf("a", "b").radiateFrom(0)).containsExactly("a", "b")
    assertThat(listOf("a", "b").radiateFrom(1)).containsExactly("b", "a")
    assertThat(listOf("a", "b", "c").radiateFrom(0)).containsExactly("a", "b", "c")
    assertThat(listOf("a", "b", "c").radiateFrom(1)).containsExactly("b", "a", "c")
    assertThat(listOf("a", "b", "c").radiateFrom(2)).containsExactly("c", "b", "a")
    assertThat(listOf("a", "b", "c", "d", "e", "f", "g", "h").radiateFrom(3))
      .containsExactly("d", "c", "e", "b", "f", "a", "g", "h")
  }
}
