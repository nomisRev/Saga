package io.github.nomisrev.internal

import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

public actual class AtomicRef<V> actual constructor(initialValue: V) {
  private val atom = AtomicReference(initialValue.freeze())
  public actual fun get(): V = atom.value
  public actual fun compareAndSet(expected: V, new: V): Boolean = atom.compareAndSet(expected, new)
}
