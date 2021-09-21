package io.github.nomisrev

import arrow.core.nonFatalOrThrow
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/* A variant of [ExitCase] that contains value of [A] in case of [ExitCase.Completed] */
internal sealed class ExitCase<out A> {
    data class Completed<A>(val value: A) : ExitCase<A>() {
        override fun toString(): String =
            "ExitCase.Completed"
    }

    data class Cancelled(val exception: CancellationException) : ExitCase<Nothing>()
    data class Failure(val failure: Throwable) : ExitCase<Nothing>()
}

/*  A variant of guaranteeCase that contains value of [A] in case of [ExitCase.Completed] */
@Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
internal suspend inline fun <A> guaranteeCase(
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

@PublishedApi
internal suspend inline fun runReleaseAndRethrow(original: Throwable, crossinline f: suspend () -> Unit): Nothing {
    try {
        withContext(NonCancellable) {
            f()
        }
    } catch (e: Throwable) {
        original.addSuppressed(e.nonFatalOrThrow())
    }
    throw original
}