@file:JvmName("AtomicRefJVM")

package io.github.nomisrev.internal

import java.util.concurrent.atomic.AtomicReference

public actual typealias AtomicRef<V> = AtomicReference<V>
