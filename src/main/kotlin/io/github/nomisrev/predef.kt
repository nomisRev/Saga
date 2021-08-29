@file:Suppress("UNUSED", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package io.github.nomisrev

import arrow.core.Either
import arrow.fx.coroutines.nonFatalOrThrow
import arrow.fx.coroutines.runReleaseAndRethrow
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal fun <E, A> Either<E, A>.leftOrNull(): E? = when (this) {
    is Either.Left -> value
    is Either.Right -> null
}

sealed class ExitCase<out A> {
    data class Completed<A>(val value: A) : ExitCase<A>() {
        override fun toString(): String =
            "ExitCase.Completed"
    }

    data class Cancelled(val exception: CancellationException) : ExitCase<Nothing>()
    data class Failure(val failure: Throwable) : ExitCase<Nothing>()
}

@Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
suspend inline fun <A> guaranteeCase(
    fa: suspend () -> A,
    crossinline finalizer: suspend (ExitCase<A>) -> Unit
): A {
    val res = try {
        fa()
    } catch (e: CancellationException) {
        runReleaseAndRethrow(e) { finalizer(ExitCase.Cancelled(e)) }
    } catch (t: Throwable) {
        runReleaseAndRethrow(t.nonFatalOrThrow()) { finalizer(ExitCase.Failure(t.nonFatalOrThrow())) }
    }
    withContext(NonCancellable) { finalizer(ExitCase.Completed(res)) }
    return res
}
