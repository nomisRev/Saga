package io.github.nomisrev.internal

public expect class AtomicRef<V>(initialValue: V) {
  internal fun get(): V
  internal fun compareAndSet(expected: V, new: V): Boolean
}

internal fun <V> AtomicRef<V>.updateAndGet(f: (V) -> V): V {
  while (true) {
    val cur = get()
    val upd = f(cur)
    if (compareAndSet(cur, upd)) return upd
  }
}
