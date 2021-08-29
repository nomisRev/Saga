package io.github.nomisrev

import arrow.core.nonFatalOrThrow
import arrow.core.prependTo
import arrow.fx.coroutines.Platform
import arrow.fx.coroutines.parTraverse
import arrow.fx.coroutines.parZip
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * The saga design pattern is a way to manage data consistency across microservices in distributed transaction scenarios.
 * A [Saga] is useful when you need to manage data in a consistent manner across services in distributed transaction scenarios.
 * Or when you need to compose multiple [action]s with a [compensation] that needs to run in a transaction like style.
 *
 * For example, let's say that we have following domain types `Order`, `Payment`.
 *
 * ```kotlin
 * data class Order(val id: UUID, val amount: Long)
 * data class Payment(val id: UUID, val orderId: UUID)
 * ```
 *
 * The creation of an `Order` can only remain when a payment has been made.
 * In SQL you might run this inside a transaction, which can automatically rollback the creation of the `Order`
 * when the creation of the Payment fails.
 *
 * When you need to do this across distributed services, or a multiple atomic references, etc
 * you need to manually facilitate the rolling back of the performed actions, or compensating actions.
 *
 * The [Saga] type, and [saga] DSL remove all the boilerplate of manually having to facilitate this with a convenient suspending DSL.
 *
 * ```kotlin
 * data class Order(val id: UUID, val amount: Long)
 * suspend fun createOrder(): Order = Order(UUID.randomUUID(), 100L)
 * suspend fun deleteOrder(order: Order): Unit = println("Deleting $order")
 *
 * data class Payment(val id: UUID, val orderId: UUID)
 * suspend fun createPayment(order: Order): Payment = Payment(UUID.randomUUID(), order.id)
 * suspend fun deletePayment(payment: Payment): Unit = println("Deleting $payment")
 *
 * suspend fun Payment.awaitSuccess(): Unit = throw RuntimeException("Payment Failed")
 *
 * suspend fun main() {
 *   saga {
 *     val order = saga { createOrder() }.compensate(::deleteOrder).bind()
 *     val payment = saga { createPayment(order) }.compensate(::deletePayment).bind()
 *     payment.awaitSuccess()
 *   }.transact()
 * }
 * ```
 */
sealed interface Saga<A> {

    fun <B, C> parZip(other: Saga<B>, f: suspend CoroutineScope.(A, B) -> C): Saga<C> =
        parZip(Dispatchers.Default, other, f)

    fun <B, C> parZip(
        ctx: CoroutineContext,
        other: Saga<B>,
        f: suspend CoroutineScope.(A, B) -> C
    ): Saga<C> = saga {
        parZip(ctx, { bind() }, { other.bind() }, f)
    }

    fun <B, C, D> parZip(
        ctx: CoroutineContext,
        b: Saga<B>,
        c: Saga<C>,
        f: suspend CoroutineScope.(A, B, C) -> D
    ): Saga<D> = saga {
        parZip(ctx, { bind() }, { b.bind() }, { c.bind() }, f)
    }

    fun <B, C, D, E> parZip(
        ctx: CoroutineContext,
        b: Saga<B>,
        c: Saga<C>,
        d: Saga<D>,
        f: suspend CoroutineScope.(A, B, C, D) -> E
    ): Saga<E> = saga {
        parZip(ctx, { bind() }, { b.bind() }, { c.bind() }, { d.bind() }, f)
    }

    fun <B, C, D, E, F> parZip(
        ctx: CoroutineContext,
        b: Saga<B>,
        c: Saga<C>,
        d: Saga<D>,
        e: Saga<E>,
        f: suspend CoroutineScope.(A, B, C, D, E) -> F
    ): Saga<F> = saga {
        parZip(ctx, { bind() }, { b.bind() }, { c.bind() }, { d.bind() }, { e.bind() }, f)
    }

    fun <B, C, D, E, F, G> parZip(
        ctx: CoroutineContext,
        b: Saga<B>,
        c: Saga<C>,
        d: Saga<D>,
        e: Saga<E>,
        ff: Saga<F>,
        f: suspend CoroutineScope.(A, B, C, D, E, F) -> G
    ): Saga<G> = saga {
        parZip(ctx, { bind() }, { b.bind() }, { c.bind() }, { d.bind() }, { e.bind() }, { ff.bind() }, f)
    }

    fun <B, C, D, E, F, G, H> parZip(
        ctx: CoroutineContext,
        b: Saga<B>,
        c: Saga<C>,
        d: Saga<D>,
        e: Saga<E>,
        ff: Saga<F>,
        g: Saga<G>,
        f: suspend CoroutineScope.(A, B, C, D, E, F, G) -> H
    ): Saga<H> = saga {
        parZip(ctx, { bind() }, { b.bind() }, { c.bind() }, { d.bind() }, { e.bind() }, { ff.bind() }, { g.bind() }, f)
    }

    fun <B, C, D, E, F, G, H, I> parZip(
        ctx: CoroutineContext,
        b: Saga<B>,
        c: Saga<C>,
        d: Saga<D>,
        e: Saga<E>,
        ff: Saga<F>,
        g: Saga<G>,
        h: Saga<H>,
        f: suspend CoroutineScope.(A, B, C, D, E, F, G, H) -> I
    ): Saga<I> = saga {
        parZip(
            ctx,
            { bind() },
            { b.bind() },
            { c.bind() },
            { d.bind() },
            { e.bind() },
            { ff.bind() },
            { g.bind() },
            { h.bind() },
            f
        )
    }

    infix fun compensate(compensate: suspend (A) -> Unit): Saga<A> =
        when (this) {
            is Builder -> Full({ f(this) }, compensate)
            is Effect -> saga {
                saga { f(this) }.compensate(compensate).bind()
            }
            is Full -> Full(action) { a ->
                compensate(a)
                this.compensation(a)
            }
            is Part -> Full(action, compensate)
        }

    suspend fun transact(): A =
        when (this) {
            is Effect -> saga(f).transact()
            is Full -> saga { bind() }.transact()
            is Part -> saga { action(this) }.transact()
            is Builder -> _transact()
        }

    @JvmInline
    value class Part<A>(val action: suspend SagaEffect.() -> A) : Saga<A> {
        override infix fun compensate(compensate: suspend (A) -> Unit): Saga<A> = Full(action, compensate)
    }

    class Full<A>(val action: suspend SagaEffect.() -> A, val compensation: suspend (A) -> Unit) : Saga<A>

    class Builder<A>(val f: suspend SagaEffect.() -> A) : Saga<A> {
        internal suspend fun _transact(): A {
            val builder = SagaBuilder()
            return guaranteeCase({ f(builder) }) { exitCase ->
                when (exitCase) {
                    is ExitCase.Completed -> Unit
                    else -> builder.totalCompensation()
                }
            }
        }
    }

    class Effect<A>(val f: suspend SagaEffect.() -> A) : Saga<A>

}

fun <A, B> Iterable<A>.traverseSaga(f: (A) -> Saga<B>): Saga<List<B>> =
    if (this is Collection && this.isEmpty()) Saga.Part { emptyList() }
    else saga { map { f(it).bind() } }

fun <A> Iterable<Saga<A>>.sequence(): Saga<List<A>> =
    if (this is Collection && this.isEmpty()) Saga.Part { emptyList() }
    else saga { map { it.bind() } }

fun <A, B> Iterable<A>.parTraverseSaga(f: (A) -> Saga<B>): Saga<List<B>> =
    parTraverseSaga(Dispatchers.Default, f)

fun <A, B> Iterable<A>.parTraverseSaga(ctx: CoroutineContext, f: (A) -> Saga<B>): Saga<List<B>> =
    if (this is Collection && this.isEmpty()) Saga.Part { emptyList() }
    else saga { parTraverse(ctx) { f(it).bind() } }

fun <A> Iterable<Saga<A>>.parSequence(ctx: CoroutineContext = Dispatchers.Default): Saga<List<A>> =
    if (this is Collection && this.isEmpty()) Saga.Part { emptyList() }
    else saga { parTraverse(ctx) { it.bind() } }

interface SagaEffect {
    suspend fun <A> Saga<A>.bind(): A
    fun <A> saga(action: suspend SagaEffect.() -> A): Saga.Part<A> = Saga.Part(action)
}

fun <A> saga(block: suspend SagaEffect.() -> A): Saga<A> = Saga.Builder(block)

@PublishedApi
internal class SagaBuilder(private val stack: AtomicReference<List<suspend () -> Unit>> = AtomicReference(emptyList())) :
    SagaEffect {

    override suspend fun <A> Saga<A>.bind(): A =
        when (this) {
            is Saga.Effect -> f(this@SagaBuilder)
            is Saga.Full -> bind()
            is Saga.Part -> action()
            is Saga.Builder -> bind()
        }

    private suspend fun <A> Saga.Full<A>.bind() =
        guaranteeCase({ action() }) { exitCase ->
            when (exitCase) {
                is ExitCase.Completed<A> -> stack.updateAndGet { suspend { compensation(exitCase.value) } prependTo it }
                // This action failed so we have no compensate to push on the stack
                // the compensate stack will run in the `transact` stage, this is just the builder
                is ExitCase.Cancelled -> Unit
                is ExitCase.Failure -> Unit
            }
        }

    private suspend fun <A> Saga.Builder<A>.bind() =
        f(this@SagaBuilder)

    @PublishedApi
    internal suspend fun totalCompensation(): Unit {
        val errors = stack.get().mapNotNull { finalizer ->
            try {
                finalizer()
                null
            } catch (e: Throwable) {
                e.nonFatalOrThrow()
            }
        }
        Platform.composeErrors(errors)?.let { throw it }
    }
}
