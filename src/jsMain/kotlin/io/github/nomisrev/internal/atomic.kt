package io.github.nomisrev.internal

public actual class AtomicRef<V> actual constructor(initialValue: V) {
  private var internalValue: V = initialValue

  /**
   * Compare current value with expected and set to new if they're the same. Note, 'compare' is
   * checking the actual object id, not 'equals'.
   */
  public actual fun compareAndSet(expected: V, new: V): Boolean =
    if (expected === internalValue) {
      internalValue = new
      true
    } else {
      false
    }

  public actual fun get(): V = internalValue
}
